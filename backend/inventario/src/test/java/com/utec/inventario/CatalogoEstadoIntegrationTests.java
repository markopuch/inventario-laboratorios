package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(60)
class CatalogoEstadoIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ObjectMapper mapper;
    @LocalServerPort private int port;

    private HttpClient client;
    private String token;
    private String run;
    private int categoria;
    private int subcategoria;
    private int sede;
    private int area;
    private int laboratorio;

    @BeforeEach
    void preparar() {
        client = HttpClient.newHttpClient();
        token = tokenDe("marko");
        run = UUID.randomUUID().toString().substring(0, 18);
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria",
                Integer.class, "Estados " + run);
        subcategoria = jdbc.queryForObject(
                "INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria",
                Integer.class, "Subcategoría " + run, categoria);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede",
                Integer.class, "Estados " + run);
        area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area",
                Integer.class, "Estados " + run, sede);
        laboratorio = jdbc.queryForObject(
                "INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio",
                Integer.class, "Estados " + run, "EST-" + run, area);
    }

    @AfterEach
    void limpiarSoloFixtures() {
        if (client != null) client.close();
        jdbc.update("DELETE FROM usuario_laboratorio WHERE id_laboratorio IN "
                + "(SELECT id_laboratorio FROM laboratorio WHERE id_area = ?)", area);
        jdbc.update("DELETE FROM equipo WHERE id_laboratorio IN "
                + "(SELECT id_laboratorio FROM laboratorio WHERE id_area = ?)", area);
        jdbc.update("DELETE FROM laboratorio WHERE id_area = ?", area);
        jdbc.update("DELETE FROM area WHERE id_area = ?", area);
        jdbc.update("DELETE FROM sede WHERE id_sede = ?", sede);
        jdbc.update("DELETE FROM subcategoria WHERE id_categoria = ?", categoria);
        jdbc.update("DELETE FROM categoria WHERE id_categoria = ?", categoria);
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoria", "subcategoria", "sede", "area", "laboratorio"})
    void adminPuedeDesactivarListarInactivosYReactivarSinCambiarGetNormal(String tipo) throws Exception {
        quitarHijosActivos(tipo);
        int id = idDe(tipo);
        String base = "/api/" + plural(tipo);
        JsonNode original = correcto(200, enviar("GET", base + "/" + id, token, null));
        JsonNode inactivo = cambiar(tipo, id, false, 200);
        assertFalse(inactivo.path("activo").asBoolean());
        assertFalse(contiene(correcto(200, enviar("GET", base, token, null)), id));
        assertEquals(404, enviar("GET", base + "/" + id, token, null).statusCode());
        assertTrue(contiene(correcto(200, enviar("GET", "/api/admin/" + plural(tipo), token, null)), id));
        assertTrue(contiene(correcto(200, enviar("GET", "/api/admin/" + plural(tipo) + "?activo=false", token, null)), id));
        assertFalse(contiene(correcto(200, enviar("GET", "/api/admin/" + plural(tipo) + "?activo=true", token, null)), id));
        JsonNode activo = cambiar(tipo, id, true, 200);
        assertTrue(activo.path("activo").asBoolean());
        assertEquals(original.path("fechaCreacion"), activo.path("fechaCreacion"));
        assertEquals(original.path("nombre"), activo.path("nombre"));
        assertTrue(contiene(correcto(200, enviar("GET", base, token, null)), id));
        assertEquals(activo, cambiar(tipo, id, true, 200), "Repetir el estado no modifica identidad ni fecha");
    }

    @ParameterizedTest
    @CsvSource({"subcategoria,categoria", "area,sede", "laboratorio,area"})
    void reactivarHijoExigePadreActivo(String hijo, String padre) throws Exception {
        quitarHijosActivos(hijo);
        cambiar(hijo, idDe(hijo), false, 200);
        cambiar(padre, idDe(padre), false, 200);
        cambiar(hijo, idDe(hijo), true, 409);
        assertFalse(activo(hijo));
        cambiar(padre, idDe(padre), true, 200);
        cambiar(hijo, idDe(hijo), true, 200);
        assertTrue(activo(hijo));
    }

    @Test
    void patchRespetaTodosLosBloqueosPorHijosEquiposYAsignaciones() throws Exception {
        cambiar("categoria", categoria, false, 409);
        cambiar("sede", sede, false, 409);
        cambiar("area", area, false, 409);
        int equipo = jdbc.queryForObject("""
                INSERT INTO equipo(codigo_interno,nombre,id_subcategoria,id_laboratorio)
                VALUES (?,?,?,?) RETURNING id_equipo
                """, Integer.class, "EST-EQ-" + run, "Equipo vigente", subcategoria, laboratorio);
        cambiar("subcategoria", subcategoria, false, 409);
        cambiar("laboratorio", laboratorio, false, 409);
        assertTrue(activo("subcategoria"));
        assertTrue(activo("laboratorio"));
        jdbc.update("UPDATE equipo SET estado = 'BAJA' WHERE id_equipo = ?", equipo);
        int usuario = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE username = 'marko'", Integer.class);
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?)", usuario, laboratorio);
        cambiar("laboratorio", laboratorio, false, 409);
        jdbc.update("UPDATE usuario_laboratorio SET activo = FALSE WHERE id_usuario = ? AND id_laboratorio = ?",
                usuario, laboratorio);
        cambiar("subcategoria", subcategoria, false, 200);
        cambiar("laboratorio", laboratorio, false, 200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoria", "subcategoria", "sede", "area", "laboratorio"})
    void consultasAdminYCambiosDeEstadoExigenAdminYJwt(String tipo) throws Exception {
        String listado = "/api/admin/" + plural(tipo);
        String estado = "/api/" + plural(tipo) + "/" + idDe(tipo) + "/estado";
        assertEquals(401, enviar("GET", listado, null, null).statusCode());
        assertEquals(401, enviar("PATCH", estado, null, "{\"activo\":true}").statusCode());
        for (String usuario : new String[] {"aldo", "romel"}) {
            String otro = tokenDe(usuario);
            assertEquals(403, enviar("GET", listado, otro, null).statusCode());
            assertEquals(403, enviar("PATCH", estado, otro, "{\"activo\":true}").statusCode());
            assertEquals(200, enviar("GET", "/api/" + plural(tipo), otro, null).statusCode());
        }
    }

    @Test
    void requestDeEstadoValidaCampoObligatorioYRecursoInexistente() throws Exception {
        String path = "/api/laboratorios/" + laboratorio + "/estado";
        assertEquals(400, enviar("PATCH", path, token, "{}").statusCode());
        assertEquals(400, enviar("PATCH", path, token, "{\"activo\":null}").statusCode());
        assertEquals(400, enviar("PATCH", "/api/laboratorios/0/estado", token, "{\"activo\":true}").statusCode());
        assertEquals(404, enviar("PATCH", "/api/laboratorios/2147483647/estado", token,
                "{\"activo\":true}").statusCode());
        assertEquals(400, enviar("GET", "/api/admin/laboratorios?activo=incorrecto", token, null).statusCode());
        assertTrue(activo("laboratorio"));
    }

    @Test
    void estadoOperativoEsIndependienteDeBajaYPutAntiguoLoConserva() throws Exception {
        Map<String, Object> datos = Map.of("nombre", "Laboratorio operativo", "codigo", "EST-API-" + run,
                "idArea", area);
        JsonNode creado = correcto(201, enviar("POST", "/api/laboratorios", token, mapper.writeValueAsString(datos)));
        int id = creado.path("id").asInt();
        assertEquals("OPERATIVO", creado.path("estadoOperativo").asString());
        String path = "/api/laboratorios/" + id;
        JsonNode mantenimiento = correcto(200, enviar("PUT", path, token, mapper.writeValueAsString(
                Map.of("nombre", "Laboratorio operativo", "codigo", "EST-API-" + run,
                        "idArea", area, "estadoOperativo", "MANTENIMIENTO"))));
        assertEquals("MANTENIMIENTO", mantenimiento.path("estadoOperativo").asString());
        assertTrue(mantenimiento.path("activo").asBoolean());
        JsonNode actualizado = correcto(200, enviar("PUT", path, token, mapper.writeValueAsString(datos)));
        assertEquals("MANTENIMIENTO", actualizado.path("estadoOperativo").asString());
        JsonNode baja = cambiar("laboratorio", id, false, 200);
        assertEquals("MANTENIMIENTO", baja.path("estadoOperativo").asString());
        JsonNode reactivado = cambiar("laboratorio", id, true, 200);
        assertEquals("MANTENIMIENTO", reactivado.path("estadoOperativo").asString());
        assertTrue(reactivado.path("activo").asBoolean());
        JsonNode operativo = correcto(200, enviar("PUT", path, token, mapper.writeValueAsString(
                Map.of("nombre", "Laboratorio operativo", "codigo", "EST-API-" + run,
                        "idArea", area, "estadoOperativo", "OPERATIVO"))));
        assertEquals("OPERATIVO", operativo.path("estadoOperativo").asString());
    }

    @Test
    void crearConEstadoOperativoExplicitoYRechazarValorDesconocido() throws Exception {
        String body = mapper.writeValueAsString(Map.of("nombre", "En mantenimiento", "codigo", "EST-M-" + run,
                "idArea", area, "estadoOperativo", "MANTENIMIENTO"));
        JsonNode creado = correcto(201, enviar("POST", "/api/laboratorios", token, body));
        assertEquals("MANTENIMIENTO", creado.path("estadoOperativo").asString());
        assertEquals(400, enviar("POST", "/api/laboratorios", token,
                body.replace("MANTENIMIENTO", "DESCONOCIDO")).statusCode());
        assertEquals(400, enviar("PUT", "/api/laboratorios/" + laboratorio, token,
                body.replace("MANTENIMIENTO", "DESCONOCIDO")).statusCode());
        assertEquals("OPERATIVO", jdbc.queryForObject(
                "SELECT estado_operativo FROM laboratorio WHERE id_laboratorio = ?", String.class, laboratorio));
    }

    @Test
    void migracionV10AplicaDefaultNotNullYCheckDeEstadoOperativo() {
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '10' AND success", Integer.class));
        assertEquals("OPERATIVO", jdbc.queryForObject(
                "SELECT estado_operativo FROM laboratorio WHERE id_laboratorio = ?", String.class, laboratorio));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE laboratorio SET estado_operativo = 'DESCONOCIDO' WHERE id_laboratorio = ?", laboratorio));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE laboratorio SET estado_operativo = NULL WHERE id_laboratorio = ?", laboratorio));
    }

    @ParameterizedTest
    @CsvSource({"subcategoria,categoria", "area,sede", "laboratorio,area"})
    void reactivacionYBajaDelPadreCompartenBloqueoEnAmbosOrdenes(String hijo, String padre) throws Exception {
        quitarHijosActivos(hijo);
        for (boolean padrePrimero : new boolean[] {false, true}) {
            if (!activo(padre)) cambiar(padre, idDe(padre), true, 200);
            if (activo(hijo)) cambiar(hijo, idDe(hijo), false, 200);
            try (Connection externo = dataSource.getConnection()) {
                externo.setAutoCommit(false);
                try (PreparedStatement lock = externo.prepareStatement(
                        "SELECT id_" + padre + " FROM " + padre + " WHERE id_" + padre + " = ? FOR UPDATE")) {
                    lock.setInt(1, idDe(padre));
                    try (var rows = lock.executeQuery()) { assertTrue(rows.next()); }
                }
                HttpRequest reactivar = solicitud("PATCH", "/api/" + plural(hijo) + "/" + idDe(hijo) + "/estado",
                        token, "{\"activo\":true}");
                HttpRequest desactivar = solicitud("PATCH", "/api/" + plural(padre) + "/" + idDe(padre) + "/estado",
                        token, "{\"activo\":false}");
                CompletableFuture<HttpResponse<String>> primero = client.sendAsync(
                        padrePrimero ? desactivar : reactivar, HttpResponse.BodyHandlers.ofString());
                esperarBloqueos(1);
                CompletableFuture<HttpResponse<String>> segundo = client.sendAsync(
                        padrePrimero ? reactivar : desactivar, HttpResponse.BodyHandlers.ofString());
                esperarBloqueos(2);
                externo.commit();
                HttpResponse<String> primeraRespuesta = primero.get(20, TimeUnit.SECONDS);
                HttpResponse<String> segundaRespuesta = segundo.get(20, TimeUnit.SECONDS);
                assertEquals(200, primeraRespuesta.statusCode(), primeraRespuesta.body());
                assertEquals(409, segundaRespuesta.statusCode(), segundaRespuesta.body());
                assertEquals(!padrePrimero, activo(hijo));
                assertEquals(!padrePrimero, activo(padre));
            }
        }
    }

    private void quitarHijosActivos(String tipo) throws Exception {
        if (tipo.equals("sede") || tipo.equals("area")) cambiar("laboratorio", laboratorio, false, 200);
        if (tipo.equals("sede")) cambiar("area", area, false, 200);
        if (tipo.equals("categoria")) cambiar("subcategoria", subcategoria, false, 200);
    }

    private int idDe(String tipo) {
        return switch (tipo) {
            case "categoria" -> categoria;
            case "subcategoria" -> subcategoria;
            case "sede" -> sede;
            case "area" -> area;
            case "laboratorio" -> laboratorio;
            default -> throw new IllegalArgumentException("Tipo de fixture desconocido");
        };
    }

    private String plural(String tipo) {
        return tipo.equals("sede") ? "sedes" : tipo + "s";
    }

    private boolean activo(String tipo) {
        // Los nombres de tabla/columna solo provienen de los literales del test.
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT activo FROM " + tipo
                + " WHERE id_" + tipo + " = ?", Boolean.class, idDe(tipo)));
    }

    private JsonNode cambiar(String tipo, int id, boolean activo, int esperado) throws Exception {
        return correcto(esperado, enviar("PATCH", "/api/" + plural(tipo) + "/" + id + "/estado", token,
                mapper.writeValueAsString(Map.of("activo", activo))));
    }

    private String tokenDe(String userName) {
        return jwtService.generateToken((UserInfoDetails) usuarioService.loadUserByUsername(userName));
    }

    private HttpResponse<String> enviar(String method, String path, String bearer, String body) throws Exception {
        return client.send(solicitud(method, path, bearer, body), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest solicitud(String method, String path, String bearer, String body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
        if (bearer != null) request.header("Authorization", "Bearer " + bearer);
        if (body != null) request.header("Content-Type", "application/json");
        return request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private JsonNode correcto(int esperado, HttpResponse<String> response) {
        assertEquals(esperado, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    private boolean contiene(JsonNode rows, int id) {
        assertTrue(rows.isArray());
        for (JsonNode row : rows) if (row.path("id").asInt() == id) return true;
        return false;
    }

    private void esperarBloqueos(int minimo) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        int bloqueados = 0;
        while (System.nanoTime() < deadline) {
            bloqueados = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND usename = current_user
                      AND state = 'active' AND wait_event_type = 'Lock'
                      AND (query ILIKE '%sede%' OR query ILIKE '%area%'
                           OR query ILIKE '%laboratorio%' OR query ILIKE '%categoria%')
                      AND pid <> pg_backend_pid()
                    """, Integer.class);
            if (bloqueados >= minimo) return;
            Thread.sleep(25);
        }
        fail("Se esperaban " + minimo + " solicitudes bloqueadas; hubo " + bloqueados);
    }
}
