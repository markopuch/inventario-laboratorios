package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(90)
class AdminUsuarioIntegrationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String BASE = "/api/admin/usuarios";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private ObjectMapper mapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @LocalServerPort private int port;

    private final List<Integer> usuarios = new ArrayList<>();
    private HttpClient client;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.tokenAdmin = tokenDe("marko");
    }

    @AfterEach
    void limpiarFixturesPropios() {
        this.client.close();
        for (Integer id : usuarios) {
            this.jdbc.update("DELETE FROM auditoria WHERE (entidad IN ('usuario','usuario_laboratorio') "
                    + "AND id_entidad = ?) OR id_usuario_actor = ?", id, id);
            this.jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario = ?", id);
            this.jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id);
        }
    }

    @Test
    void exigeJwtParaTodoElContratoAdministrativo() throws Exception {
        Integer id = idMarko();
        assertError(401, enviar("GET", BASE, null, null));
        assertError(401, enviar("GET", BASE + "/" + id, null, null));
        assertError(401, enviar("POST", BASE, null, Map.of()));
        assertError(401, enviar("PUT", BASE + "/" + id, null, Map.of()));
        assertError(401, enviar("PATCH", BASE + "/" + id + "/estado", null, Map.of("activo", false)));
        assertError(401, enviar("PATCH", BASE + "/" + id + "/rol", null, Map.of("rol", "LECTOR")));
        assertError(401, enviar("PUT", BASE + "/" + id + "/password", null, Map.of("password", "1234")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aldo", "romel"})
    void gestorYLectorNoAdministranUsuarios(String userName) throws Exception {
        String token = tokenDe(userName);
        Integer id = idMarko();
        assertError(403, enviar("GET", BASE, token, null));
        assertError(403, enviar("GET", BASE + "/" + id, token, null));
        assertError(403, enviar("POST", BASE, token, Map.of()));
        assertError(403, enviar("PUT", BASE + "/" + id, token, Map.of()));
        assertError(403, enviar("PATCH", BASE + "/" + id + "/estado", token, Map.of("activo", false)));
        assertError(403, enviar("PATCH", BASE + "/" + id + "/rol", token, Map.of("rol", "LECTOR")));
        assertError(403, enviar("PUT", BASE + "/" + id + "/password", token, Map.of("password", "1234")));
    }

    @Test
    void creaConsultaEditaEIncluyeInactivosSinExponerCredenciales() throws Exception {
        String run = identificador();
        String userName = "admin_test_" + run;
        String password = " clave " + run + " ";
        JsonNode creado = crear(userName.toUpperCase(Locale.ROOT), "LECTOR", password, true);
        Integer id = creado.path("id").asInt();
        assertEquals(userName, creado.path("userName").asString());
        assertEquals("Docente", creado.path("cargo").asString());
        assertEquals(200, login(userName, password).statusCode());
        assertEquals(401, login(userName, password.trim()).statusCode(), "La contraseña no se recorta");

        HttpResponse<String> editado = enviar("PUT", BASE + "/" + id, tokenAdmin,
                Map.of("nombre", "Nombre editado", "apellido", "Apellido editado",
                        "email", "editar_" + run + "@inventario.test", "cargo", "Técnico"));
        assertEquals(200, editado.statusCode(), editado.body());
        JsonNode usuario = mapper.readTree(editado.body());
        assertEquals(userName, usuario.path("userName").asString());
        assertEquals("Técnico", usuario.path("cargo").asString());
        assertEquals(creado.path("fechaCreacion"), usuario.path("fechaCreacion"));
        assertPublico(editado.body());
        assertEquals(200, enviar("PATCH", BASE + "/" + id + "/estado", tokenAdmin,
                Map.of("activo", false)).statusCode());
        HttpResponse<String> detalle = enviar("GET", BASE + "/" + id, tokenAdmin, null);
        assertEquals(200, detalle.statusCode());
        assertFalse(mapper.readTree(detalle.body()).path("activo").asBoolean());
        HttpResponse<String> lista = enviar("GET", BASE, tokenAdmin, null);
        assertEquals(200, lista.statusCode());
        assertPublico(lista.body());
        boolean encontrado = false;
        for (JsonNode item : mapper.readTree(lista.body())) {
            if (item.path("id").asInt() == id) encontrado = true;
        }
        assertTrue(encontrado, "El listado ADMIN incluye inactivos");
    }

    @Test
    void duplicadosUsernameYEmailIgnoranMayusculasInclusoEnUsuariosInactivos() throws Exception {
        String run = identificador();
        String userName = "duplicado_" + run;
        JsonNode creado = crear(userName, "LECTOR", "1234", false);
        String email = creado.path("email").asString();
        assertError(409, enviar("POST", BASE, tokenAdmin,
                bodyCrear(userName.toUpperCase(Locale.ROOT), "otro_" + run + "@inventario.test", "LECTOR", "1234", true)));
        assertError(409, enviar("POST", BASE, tokenAdmin,
                bodyCrear("otro_" + run, email.toUpperCase(Locale.ROOT), "LECTOR", "1234", true)));
        JsonNode otro = crear("distinto_" + run, "GESTOR", "1234", true);
        assertError(409, enviar("PUT", BASE + "/" + otro.path("id").asInt(), tokenAdmin,
                Map.of("nombre", "Prueba", "apellido", "Prueba", "email", email.toUpperCase(Locale.ROOT))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"passwordHash", "userName", "activo", "rol", "id"})
    void elPutRechazaCamposNoEditables(String campo) throws Exception {
        JsonNode creado = crear("protegido_" + identificador(), "LECTOR", "1234", true);
        var body = new java.util.HashMap<String, Object>();
        body.put("nombre", "Prueba");
        body.put("apellido", "Prueba");
        body.put("email", creado.path("email").asString());
        body.put(campo, "valor-no-editable");
        assertError(400, enviar("PUT", BASE + "/" + creado.path("id").asInt(), tokenAdmin, body));
    }

    @Test
    void validaCreacionYRechazaHashRolesInvalidosYPasswordDeMasDe72Bytes() throws Exception {
        assertError(400, enviar("POST", BASE, tokenAdmin, Map.of()));
        String run = identificador();
        var body = new java.util.HashMap<>(bodyCrear("validacion_" + run,
                run + "@inventario.test", "LECTOR", "1234", true));
        body.put("passwordHash", "hash-cliente");
        assertError(400, enviar("POST", BASE, tokenAdmin, body));
        body.remove("passwordHash");
        body.put("rol", "SUPERADMIN");
        assertError(400, enviar("POST", BASE, tokenAdmin, body));
        body.put("rol", "LECTOR");
        body.put("password", "ñ".repeat(37));
        assertError(400, enviar("POST", BASE, tokenAdmin, body));
        body.put("password", "abc");
        assertError(400, enviar("POST", BASE, tokenAdmin, body));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario WHERE username = ?",
                Integer.class, "validacion_" + run));
    }

    @Test
    void elUltimoAdminNoSeDesactivaNiDegradaYOperacionesIdempotentesSonValidas() throws Exception {
        Integer id = idMarko();
        assertEquals(1, cantidadAdministradores());
        assertError(409, enviar("PATCH", BASE + "/" + id + "/estado", tokenAdmin, Map.of("activo", false)));
        assertError(409, enviar("PATCH", BASE + "/" + id + "/rol", tokenAdmin, Map.of("rol", "GESTOR")));
        assertEquals(200, enviar("PATCH", BASE + "/" + id + "/estado", tokenAdmin,
                Map.of("activo", true)).statusCode());
        assertEquals(200, enviar("PATCH", BASE + "/" + id + "/rol", tokenAdmin,
                Map.of("rol", "ADMIN")).statusCode());
        assertEquals(1, cantidadAdministradores());
    }

    @Test
    void rechazaRolesInactivosSinCambiarElUsuario() throws Exception {
        JsonNode creado = crear("rol_" + identificador(), "GESTOR", "1234", true);
        Integer id = creado.path("id").asInt();
        try {
            jdbc.update("UPDATE rol SET activo = FALSE WHERE nombre = 'LECTOR'");
            assertError(409, enviar("PATCH", BASE + "/" + id + "/rol", tokenAdmin, Map.of("rol", "LECTOR")));
            String run = identificador();
            assertError(409, enviar("POST", BASE, tokenAdmin,
                    bodyCrear("sin_rol_" + run, run + "@inventario.test", "LECTOR", "1234", true)));
            assertEquals("GESTOR", mapper.readTree(enviar("GET", BASE + "/" + id, tokenAdmin, null).body())
                    .path("rol").asString());
        } finally {
            jdbc.update("UPDATE rol SET activo = TRUE WHERE nombre = 'LECTOR'");
        }
    }

    @Test
    void rolYEstadoModificanJwtExistenteYConservanAsignaciones() throws Exception {
        String userName = "alcance_" + identificador();
        JsonNode creado = crear(userName, "GESTOR", "1234", true);
        Integer id = creado.path("id").asInt();
        String token = mapper.readTree(login(userName, "1234").body()).path("accessToken").asString();
        Integer laboratorio = jdbc.queryForObject("SELECT MIN(id_laboratorio) FROM laboratorio WHERE activo",
                Integer.class);
        assertNotNull(laboratorio);
        assertEquals(200, enviar("PUT", BASE + "/" + id + "/laboratorios", tokenAdmin,
                Map.of("idsLaboratorio", List.of(laboratorio))).statusCode());
        assertEquals(200, enviar("PATCH", BASE + "/" + id + "/rol", tokenAdmin,
                Map.of("rol", "LECTOR")).statusCode());
        assertEquals("LECTOR", mapper.readTree(enviar("GET", "/api/auth/me", token, null).body())
                .path("rol").asString());
        assertEquals(200, enviar("PATCH", BASE + "/" + id + "/estado", tokenAdmin,
                Map.of("activo", false)).statusCode());
        assertError(401, enviar("GET", "/api/auth/me", token, null));
        assertError(401, login(userName, "1234"));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuario_laboratorio WHERE id_usuario = ? "
                + "AND activo", Integer.class, id));
        assertEquals(200, enviar("PATCH", BASE + "/" + id + "/estado", tokenAdmin,
                Map.of("activo", true)).statusCode());
        assertEquals(200, enviar("GET", "/api/auth/me/laboratorios", token, null).statusCode());
    }

    @Test
    void resetCifraLaNuevaPasswordYNoLaIncluyeEnAuditoria() throws Exception {
        String userName = "password_" + identificador();
        String anterior = UUID.randomUUID().toString();
        String nueva = UUID.randomUUID().toString();
        JsonNode creado = crear(userName, "LECTOR", anterior, true);
        Integer id = creado.path("id").asInt();
        HttpResponse<String> changed = enviar("PUT", BASE + "/" + id + "/password", tokenAdmin,
                Map.of("password", nueva));
        assertEquals(204, changed.statusCode());
        assertTrue(changed.body().isEmpty());
        assertError(401, login(userName, anterior));
        assertEquals(200, login(userName, nueva).statusCode());
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE id_usuario = ?",
                String.class, id);
        assertTrue(passwordEncoder.matches(nueva, hash));
        assertFalse(passwordEncoder.matches(anterior, hash));
        assertError(400, enviar("PUT", BASE + "/" + id + "/password", tokenAdmin,
                Map.of("password", "ñ".repeat(37))));
        List<String> eventos = jdbc.queryForList("SELECT cambios FROM auditoria WHERE entidad='usuario' "
                + "AND id_entidad = ?", String.class, id);
        assertTrue(eventos.contains("Contraseña restablecida"));
        for (String evento : eventos) {
            assertFalse(evento.contains(anterior));
            assertFalse(evento.contains(nueva));
            assertFalse(evento.contains(hash));
        }
    }

    @Test
    void inexistentesProducen404YNoSeCreaRegistroPublico() throws Exception {
        assertError(404, enviar("GET", BASE + "/2147483647", tokenAdmin, null));
        assertError(404, enviar("PATCH", BASE + "/2147483647/estado", tokenAdmin, Map.of("activo", true)));
        assertError(404, enviar("PATCH", BASE + "/2147483647/rol", tokenAdmin, Map.of("rol", "LECTOR")));
        assertError(404, enviar("PUT", BASE + "/2147483647/password", tokenAdmin, Map.of("password", "1234")));
        assertError(401, enviar("POST", "/api/auth/register", null, Map.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"estado", "rol"})
    void dosAdministradoresConcurrentesNuncaEliminanAlUltimo(String accion) throws Exception {
        JsonNode uno = crear("admin_uno_" + identificador(), "ADMIN", "1234", true);
        JsonNode dos = crear("admin_dos_" + identificador(), "ADMIN", "1234", true);
        Integer idUno = uno.path("id").asInt();
        Integer idDos = dos.path("id").asInt();
        String tokenUno = tokenDe(uno.path("userName").asString());
        String tokenDos = tokenDe(dos.path("userName").asString());
        Integer marko = idMarko();
        try {
            // Solo esta base temporal: deja exactamente los dos administradores del fixture.
            jdbc.update("UPDATE usuario SET activo = FALSE WHERE id_usuario = ?", marko);
            assertEquals(2, cantidadAdministradores());
            try (Connection external = dataSource.getConnection()) {
                external.setAutoCommit(false);
                try (var statement = external.prepareStatement("SELECT id_rol FROM rol WHERE nombre='ADMIN' FOR UPDATE")) {
                    try (var result = statement.executeQuery()) {
                        assertTrue(result.next());
                    }
                }
                Object body = accion.equals("estado") ? Map.of("activo", false) : Map.of("rol", "LECTOR");
                var primera = client.sendAsync(solicitud("PATCH", BASE + "/" + idUno + "/" + accion,
                        tokenUno, body), HttpResponse.BodyHandlers.ofString());
                var segunda = client.sendAsync(solicitud("PATCH", BASE + "/" + idDos + "/" + accion,
                        tokenDos, body), HttpResponse.BodyHandlers.ofString());
                esperarBloqueos(2);
                external.commit();
                List<Integer> statuses = List.of(primera.get(20, TimeUnit.SECONDS).statusCode(),
                        segunda.get(20, TimeUnit.SECONDS).statusCode()).stream().sorted().toList();
                assertEquals(List.of(200, 409), statuses);
                assertEquals(1, cantidadAdministradores());
            }
        } finally {
            jdbc.update("UPDATE usuario SET activo = TRUE WHERE id_usuario = ?", marko);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactivo", "degradado"})
    void permisoRevocadoMientrasEsperaNoPermiteCrearNiAuditar(String cambio) throws Exception {
        JsonNode creado = crear("admin_revocar_" + identificador(), "ADMIN", "1234", true);
        Integer actor = creado.path("id").asInt();
        String token = tokenDe(creado.path("userName").asString());
        String run = identificador();
        String userName = "rechazado_" + run;
        try (Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            try (var lock = external.prepareStatement("SELECT id_rol FROM rol WHERE nombre='ADMIN' FOR UPDATE")) {
                try (var row = lock.executeQuery()) {
                    assertTrue(row.next());
                }
            }
            var pendiente = client.sendAsync(solicitud("POST", BASE, token,
                    bodyCrear(userName, run + "@inventario.test", "LECTOR", "1234", true)),
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            String sql = cambio.equals("inactivo")
                    ? "UPDATE usuario SET activo = FALSE WHERE id_usuario = ?"
                    : "UPDATE usuario SET id_rol = (SELECT id_rol FROM rol WHERE nombre='LECTOR') WHERE id_usuario = ?";
            try (var update = external.prepareStatement(sql)) {
                update.setInt(1, actor);
                assertEquals(1, update.executeUpdate());
            }
            external.commit();
            HttpResponse<String> resultado = pendiente.get(20, TimeUnit.SECONDS);
            if (resultado.statusCode() == 201) {
                usuarios.add(mapper.readTree(resultado.body()).path("id").asInt());
            }
            assertError(cambio.equals("inactivo") ? 401 : 403, resultado);
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario WHERE username = ?",
                    Integer.class, userName));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM auditoria WHERE id_usuario_actor = ?",
                    Integer.class, actor));
        }
    }

    private JsonNode crear(String userName, String rol, String password, boolean activo) throws Exception {
        HttpResponse<String> response = enviar("POST", BASE, tokenAdmin,
                bodyCrear(userName, userName.toLowerCase(Locale.ROOT) + "@inventario.test", rol, password, activo));
        assertEquals(201, response.statusCode(), response.body());
        JsonNode node = mapper.readTree(response.body());
        usuarios.add(node.path("id").asInt());
        assertTrue(response.headers().firstValue("Location").orElse("").endsWith("/" + node.path("id").asInt()));
        assertPublico(response.body());
        return node;
    }

    private Map<String, Object> bodyCrear(String userName, String email, String rol, String password, boolean activo) {
        return Map.of("userName", userName, "nombre", "Prueba", "apellido", "Administración", "email", email,
                "cargo", "Docente", "rol", rol, "password", password, "activo", activo);
    }

    private String identificador() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private Integer idMarko() {
        return jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE username = 'marko'", Integer.class);
    }

    private int cantidadAdministradores() {
        return jdbc.queryForObject("SELECT count(*) FROM usuario u JOIN rol r USING(id_rol) "
                + "WHERE u.activo AND r.activo AND r.nombre='ADMIN'", Integer.class);
    }

    private String tokenDe(String userName) {
        return jwtService.generateToken(usuarioService.loadUserByUsername(userName));
    }

    private HttpResponse<String> login(String userName, String password) throws Exception {
        return enviar("POST", "/api/auth/login", null, Map.of("userName", userName, "password", password));
    }

    private HttpResponse<String> enviar(String metodo, String ruta, String token, Object body) throws Exception {
        return client.send(solicitud(metodo, ruta, token, body), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest solicitud(String metodo, String ruta, String token, Object body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
                .timeout(TIMEOUT).header("Accept", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.header("Content-Type", "application/json");
        return request.method(metodo, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
    }

    private void esperarBloqueos(int cantidad) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < limite) {
            Integer esperando = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity "
                    + "WHERE datname = current_database() AND state='active' AND wait_event_type='Lock'",
                    Integer.class);
            if (esperando != null && esperando >= cantidad) return;
            Thread.sleep(30);
        }
        fail("Las operaciones deben esperar el bloqueo de la fila ADMIN antes de soltarlo.");
    }

    private void assertError(int status, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        JsonNode error = mapper.readTree(response.body());
        assertEquals(status, error.path("status").asInt());
        assertFalse(error.has("trace"));
        assertPublico(response.body());
    }

    private void assertPublico(String json) {
        JsonNode body = mapper.readTree(json);
        assertFalse(body.has("password"));
        if (body.isArray()) {
            for (JsonNode item : body) assertFalse(item.has("password"));
        }
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("password_hash"));
        assertFalse(json.contains("$2a$"));
        assertFalse(json.contains("$2b$"));
    }
}
