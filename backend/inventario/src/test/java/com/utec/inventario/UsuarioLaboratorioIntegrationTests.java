package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.repository.UsuarioRepository;
import com.utec.inventario.service.AlcanceLaboratorioService;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(60)
class UsuarioLaboratorioIntegrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AlcanceLaboratorioService alcanceService;
    @LocalServerPort private int port;

    private HttpClient client;
    private String run;
    private int sede;
    private int area;
    private int labA;
    private int labB;
    private int labC;
    private int admin;
    private int gestor;
    private int lector;
    private String tokenAdmin;
    private String tokenGestor;
    private String tokenLector;
    private final List<Integer> usuariosPropios = new ArrayList<>();
    private final List<Integer> rolesPropios = new ArrayList<>();

    @BeforeEach
    void prepararFixturesPropios() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        run = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede", Integer.class, "S4E " + run);
        area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area", Integer.class, "S4E " + run, sede);
        labA = crearLaboratorio("A");
        labB = crearLaboratorio("B");
        labC = crearLaboratorio("C");
        admin = crearUsuario("ADMIN");
        gestor = crearUsuario("GESTOR");
        lector = crearUsuario("LECTOR");
        tokenAdmin = tokenDe("ADMIN");
        tokenGestor = tokenDe("GESTOR");
        tokenLector = tokenDe("LECTOR");
    }

    @AfterEach
    void limpiarSoloUsuariosYLaboratoriosDeEsteTest() {
        if (client != null) client.close();
        for (int id : usuariosPropios) {
            jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario = ?", id);
            jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id);
        }
        for (int id : rolesPropios) jdbc.update("DELETE FROM rol WHERE id_rol = ?", id);
        jdbc.update("DELETE FROM laboratorio WHERE id_area = ?", area);
        jdbc.update("DELETE FROM area WHERE id_area = ?", area);
        jdbc.update("DELETE FROM sede WHERE id_sede = ?", sede);
    }

    @Test
    void adminConsultaAsignacionesExplicitasVaciasYPublicasSinConfundirAlcanceGlobal() throws Exception {
        JsonNode response = ok(200, enviar("GET", ruta(admin), tokenAdmin, null));
        assertEquals(admin, response.path("usuario").path("id").asInt());
        assertEquals(username("ADMIN"), response.path("usuario").path("userName").asString());
        assertEquals("ADMIN", response.path("usuario").path("rol").asString());
        assertEquals(5, response.path("usuario").size(), "Solo datos públicos mínimos");
        assertTrue(response.path("laboratorios").isArray());
        assertEquals(0, response.path("laboratorios").size());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario_laboratorio WHERE id_usuario = ?", Integer.class, admin));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario WHERE id_usuario = ?", Integer.class, Integer.MAX_VALUE));
        error(404, enviar("GET", ruta(Integer.MAX_VALUE), tokenAdmin, null));
        error(404, enviar("PUT", ruta(Integer.MAX_VALUE), tokenAdmin, ids(labA)));
    }

    @Test
    void reemplazoDeduplicaDesactivaReactivaYConservaLaFechaOriginalIdempotentemente() throws Exception {
        JsonNode first = reemplazar(gestor, labB, labA, labB);
        assertIds(first.path("laboratorios"), labA, labB);
        assertEquals(3, first.path("laboratorios").get(0).size(), "Resumen público id/codigo/nombre");
        List<Map<String, Object>> original = filas(gestor);
        assertEquals(2, original.size());
        assertNotNull(original.getFirst().get("fecha_asignacion"));
        assertEquals(original, filasTrasPut(gestor, labA, labB));

        assertIds(reemplazar(gestor, labC, labB).path("laboratorios"), labB, labC);
        List<Map<String, Object>> changed = filas(gestor);
        assertEquals(3, changed.size());
        assertEquals(Boolean.FALSE, changed.get(0).get("activo"));
        assertEquals(original.get(0).get("fecha_asignacion"), changed.get(0).get("fecha_asignacion"));
        assertEquals(original.get(1).get("fecha_asignacion"), changed.get(1).get("fecha_asignacion"));

        assertIds(reemplazar(gestor).path("laboratorios"));
        assertEquals(3, filas(gestor).size(), "La baja conserva todas las filas históricas");
        for (Map<String, Object> row : filas(gestor)) assertEquals(Boolean.FALSE, row.get("activo"));
        assertIds(reemplazar(gestor, labA).path("laboratorios"), labA);
        List<Map<String, Object>> reactivated = filas(gestor);
        assertEquals(3, reactivated.size());
        assertEquals(Boolean.TRUE, reactivated.get(0).get("activo"));
        assertEquals(original.get(0).get("fecha_asignacion"), reactivated.get(0).get("fecha_asignacion"));
        assertEquals(reactivated, filasTrasPut(gestor, labA, labA));
        assertIds(ok(200, enviar("GET", ruta(gestor), tokenAdmin, null)).path("laboratorios"), labA);
    }

    @Test
    void unLaboratorioInexistenteOInactivoRevierteTodoElReemplazo() throws Exception {
        reemplazar(gestor, labA);
        List<Map<String, Object>> before = filas(gestor);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM laboratorio WHERE id_laboratorio = ?", Integer.class, Integer.MAX_VALUE));
        error(404, enviar("PUT", ruta(gestor), tokenAdmin, ids(labB, Integer.MAX_VALUE)));
        assertEquals(before, filas(gestor), "No desactiva A ni crea B antes de validar todos los destinos");
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labC, tokenAdmin, null).statusCode());
        error(409, enviar("PUT", ruta(gestor), tokenAdmin, ids(labB, labC)));
        assertEquals(before, filas(gestor));
        assertIds(ok(200, enviar("GET", ruta(gestor), tokenAdmin, null)).path("laboratorios"), labA);
    }

    @Test
    void requestValidaListaElementosJsonEIdsDeRutaPeroPermiteListaVacia() throws Exception {
        reemplazar(gestor, labA);
        List<Map<String, Object>> before = filas(gestor);
        for (String body : List.of("{}", "null", "{", "{\"idsLaboratorio\":null}",
                "{\"idsLaboratorio\":[null]}", "{\"idsLaboratorio\":[0]}", "{\"idsLaboratorio\":[-1]}",
                "{\"idsLaboratorio\":[\"abc\"]}", "{\"idsLaboratorio\":{}}")) {
            error(400, enviar("PUT", ruta(gestor), tokenAdmin, body));
            assertEquals(before, filas(gestor));
        }
        for (String id : List.of("abc", "0", "-1", "2147483648")) {
            String path = "/api/admin/usuarios/" + id + "/laboratorios";
            error(400, enviar("GET", path, tokenAdmin, null));
            error(400, enviar("PUT", path, tokenAdmin, ids(labA)));
        }
        assertIds(reemplazar(gestor).path("laboratorios"));
    }

    @Test
    void adminSiempreTieneTodosLosLaboratoriosActivosIndependientementeDeAsignaciones() throws Exception {
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labC, tokenAdmin, null).statusCode());
        TreeSet<Integer> active = new TreeSet<>(jdbc.queryForList(
                "SELECT id_laboratorio FROM laboratorio WHERE activo = TRUE", Integer.class));
        for (boolean explicitAssignment : new boolean[] { false, true }) {
            if (explicitAssignment) reemplazar(admin, labA);
            JsonNode scope = scope(tokenAdmin);
            assertTrue(scope.path("alcanceGlobal").asBoolean());
            assertEquals(active, idsDe(scope.path("laboratorios")));
            assertFalse(idsDe(scope.path("laboratorios")).contains(labC));
        }
        assertIds(ok(200, enviar("GET", ruta(admin), tokenAdmin, null)).path("laboratorios"), labA);
        assertNotEquals(1, active.size(), "La fixture debe distinguir alcance global de asignaciones explícitas");
    }

    @Test
    void gestorYLectorVenCambiosDeAlcanceConElMismoJwtYNoControlanElIdDelPrincipal() throws Exception {
        for (int user : new int[] { gestor, lector }) {
            String jwt = user == gestor ? tokenGestor : tokenLector;
            assertFalse(scope(jwt).path("alcanceGlobal").asBoolean());
            assertIds(scope(jwt).path("laboratorios"));
            reemplazar(user, labA, labB);
            assertIds(scope(jwt).path("laboratorios"), labA, labB);
            reemplazar(user, labB);
            assertIds(scope(jwt).path("laboratorios"), labB);
            JsonNode attemptedOtherUser = ok(200, enviar("GET", "/api/auth/me/laboratorios?idUsuario=" + admin,
                    jwt, null));
            assertFalse(attemptedOtherUser.path("alcanceGlobal").asBoolean());
            assertIds(attemptedOtherUser.path("laboratorios"), labB);
            reemplazar(user);
            assertIds(scope(jwt).path("laboratorios"));
        }
    }

    @Test
    void alcanceFiltraAsignacionesInactivasYLaboratoriosInactivosInclusoAnteDatosExternos() throws Exception {
        reemplazar(gestor, labA, labB, labC);
        reemplazar(gestor, labA, labB);
        // Simula datos heredados externos exclusivamente en una fila propia de la BD temporal.
        jdbc.update("UPDATE laboratorio SET activo = FALSE WHERE id_laboratorio = ?", labB);
        try {
            assertIds(scope(tokenGestor).path("laboratorios"), labA);
            assertFalse(idsDe(scope(tokenAdmin).path("laboratorios")).contains(labB));
        } finally {
            jdbc.update("UPDATE laboratorio SET activo = TRUE WHERE id_laboratorio = ?", labB);
        }
    }

    @Test
    void usuarioInactivoPuedeTenerConfiguracionPeroNoAutenticarseYBloqueaBajaDelLaboratorio() throws Exception {
        jdbc.update("UPDATE usuario SET activo = FALSE WHERE id_usuario = ?", gestor);
        assertIds(reemplazar(gestor, labA).path("laboratorios"), labA);
        assertIds(ok(200, enviar("GET", ruta(gestor), tokenAdmin, null)).path("laboratorios"), labA);
        error(401, enviar("GET", "/api/auth/me/laboratorios", tokenGestor, null));
        error(401, enviar("POST", "/api/auth/login", null,
                mapper.writeValueAsString(Map.of("userName", username("GESTOR"),
                        "password", System.getenv("DEMO_USER_PASSWORD")))));
        error(409, enviar("DELETE", "/api/laboratorios/" + labA, tokenAdmin, null));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT activo FROM laboratorio WHERE id_laboratorio = ?", Boolean.class, labA));
        reemplazar(gestor);
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labA, tokenAdmin, null).statusCode());
        assertEquals(1, filas(gestor).size());
        assertEquals(Boolean.FALSE, filas(gestor).getFirst().get("activo"));
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM laboratorio WHERE id_laboratorio = ?", Boolean.class, labA));
    }

    @Test
    void soloAdminAdministraAsignacionesPeroLosTresRolesConsultanSuAlcance() throws Exception {
        for (String jwt : List.of(tokenGestor, tokenLector)) {
            error(403, enviar("GET", ruta(gestor), jwt, null));
            error(403, enviar("PUT", ruta(gestor), jwt, ids(labA)));
            // La autorización se resuelve antes de buscar el usuario o validar el body.
            error(403, enviar("GET", ruta(Integer.MAX_VALUE), jwt, null));
            error(403, enviar("PUT", ruta(gestor), jwt, "{}"));
        }
        for (String jwt : List.of(tokenAdmin, tokenGestor, tokenLector)) scope(jwt);
        assertEquals(0, filas(gestor).size());
    }

    @Test
    void sinJwtOJwtInvalidoNingunoDeLosTresEndpointsAceptaPeticiones() throws Exception {
        for (String jwt : new String[] { null, "token-invalido" }) {
            error(401, enviar("GET", ruta(gestor), jwt, null));
            error(401, enviar("PUT", ruta(gestor), jwt, ids(labA)));
            error(401, enviar("GET", "/api/auth/me/laboratorios", jwt, null));
        }
        assertEquals(0, filas(gestor).size());
    }

    @Test
    void catalogosSiguenGlobalesAunqueGestorYLectorNoTenganAlcance() throws Exception {
        for (String jwt : List.of(tokenGestor, tokenLector)) {
            assertIds(scope(jwt).path("laboratorios"));
            for (String path : List.of("/api/laboratorios", "/api/areas/" + area + "/laboratorios")) {
                TreeSet<Integer> visible = idsDe(ok(200, enviar("GET", path, jwt, null)));
                assertTrue(visible.containsAll(List.of(labA, labB, labC)), "El catálogo permanece global");
            }
            for (String path : List.of("/api/laboratorios/" + labA, "/api/areas/" + area,
                    "/api/sedes/" + sede, "/api/sedes/" + sede + "/areas", "/api/categorias", "/api/subcategorias")) {
                ok(200, enviar("GET", path, jwt, null));
            }
        }
    }

    @Test
    void helperReutilizableCompruebaAlcanceYVigenciaEnBaseDeDatosSinConfiarEnCliente() throws Exception {
        assertTrue(alcanceService.tieneAccesoLaboratorio(admin, labA));
        assertFalse(alcanceService.tieneAccesoLaboratorio(gestor, labA));
        reemplazar(gestor, labA);
        assertTrue(alcanceService.tieneAccesoLaboratorio(gestor, labA));
        assertFalse(alcanceService.tieneAccesoLaboratorio(lector, labA));
        assertFalse(alcanceService.tieneAccesoLaboratorio(gestor, labB));
        assertFalse(alcanceService.tieneAccesoLaboratorio(admin, Integer.MAX_VALUE));
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labC, tokenAdmin, null).statusCode());
        assertFalse(alcanceService.tieneAccesoLaboratorio(admin, labC));
        assertFalse(alcanceService.tieneAccesoLaboratorio(gestor, labC));
        reemplazar(gestor);
        assertFalse(alcanceService.tieneAccesoLaboratorio(gestor, labA));
        jdbc.update("UPDATE usuario SET activo = FALSE WHERE id_usuario = ?", gestor);
        assertThrows(AccessDeniedException.class, () -> alcanceService.tieneAccesoLaboratorio(gestor, labA));
        assertThrows(AccessDeniedException.class, () -> alcanceService.obtenerAlcanceEfectivo(Integer.MAX_VALUE));
        int inactiveRole = jdbc.queryForObject("INSERT INTO rol(nombre,activo) VALUES (?,FALSE) RETURNING id_rol",
                Integer.class, "S4E_inactivo_" + run);
        rolesPropios.add(inactiveRole);
        jdbc.update("UPDATE usuario SET id_rol = ? WHERE id_usuario = ?", inactiveRole, lector);
        assertTrue(usuarioRepository.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(lector).isEmpty(),
                "El rol inactivo excluye al usuario aunque usuario.activo siga siendo true");
        assertThrows(AccessDeniedException.class, () -> alcanceService.tieneAccesoLaboratorio(lector, labA));
        error(401, enviar("GET", "/api/auth/me/laboratorios", tokenLector, null));
    }

    private int crearLaboratorio(String label) {
        return jdbc.queryForObject("INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio",
                Integer.class, "S4E " + label, "S4E-" + label + "-" + run, area);
    }

    private int crearUsuario(String role) {
        // Copia el hash dentro de PostgreSQL: no se consulta, imprime ni persiste en archivos.
        int id = jdbc.queryForObject("INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol) "
                + "SELECT ?,?,?,?,seed.password_hash,r.id_rol FROM usuario seed JOIN rol r ON r.nombre = ? "
                + "WHERE seed.username = 'marko' RETURNING id_usuario", Integer.class,
                username(role), "Prueba", "Sprint4E", username(role) + "@example.invalid", role);
        usuariosPropios.add(id);
        return id;
    }

    private String username(String role) { return "s4e_" + role.toLowerCase(java.util.Locale.ROOT) + "_" + run; }
    private String tokenDe(String role) { return jwtService.generateToken((UserInfoDetails) usuarioService.loadUserByUsername(username(role))); }
    private String ruta(int user) { return "/api/admin/usuarios/" + user + "/laboratorios"; }
    private String ids(Integer... labs) { return mapper.writeValueAsString(Map.of("idsLaboratorio", List.of(labs))); }

    private JsonNode reemplazar(int user, Integer... labs) throws Exception {
        return ok(200, enviar("PUT", ruta(user), tokenAdmin, ids(labs)));
    }

    private JsonNode scope(String jwt) throws Exception {
        JsonNode response = ok(200, enviar("GET", "/api/auth/me/laboratorios", jwt, null));
        assertTrue(response.has("alcanceGlobal"));
        assertTrue(response.path("laboratorios").isArray());
        return response;
    }

    private List<Map<String, Object>> filas(int user) {
        return jdbc.queryForList("SELECT id_laboratorio,activo,fecha_asignacion FROM usuario_laboratorio "
                + "WHERE id_usuario = ? ORDER BY id_laboratorio", user);
    }

    private List<Map<String, Object>> filasTrasPut(int user, Integer... labs) throws Exception {
        reemplazar(user, labs);
        return filas(user);
    }

    private TreeSet<Integer> idsDe(JsonNode laboratories) {
        assertTrue(laboratories.isArray());
        TreeSet<Integer> result = new TreeSet<>();
        for (JsonNode laboratory : laboratories) assertTrue(result.add(laboratory.path("id").asInt()), "Sin duplicados");
        return result;
    }

    private void assertIds(JsonNode laboratories, Integer... expected) {
        assertEquals(new TreeSet<>(List.of(expected)), idsDe(laboratories));
    }

    private HttpResponse<String> enviar(String method, String path, String bearer, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
        if (bearer != null) request.header("Authorization", "Bearer " + bearer);
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode ok(int status, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        for (String sensitive : List.of("passwordHash", "password_hash", "\"password\"", "$2a$", "$2b$", "JWT_SECRET")) {
            assertFalse(response.body().contains(sensitive), "El contrato no expone secretos");
        }
        return mapper.readTree(response.body());
    }

    private void error(int status, HttpResponse<String> response) {
        JsonNode error = ok(status, response);
        assertEquals(status, error.path("status").asInt());
        assertFalse(error.path("message").asString().isBlank());
        assertFalse(error.has("trace"));
        assertFalse(response.body().contains("org.hibernate"));
        assertFalse(response.body().contains("pk_usuario_laboratorio"));
    }
}
