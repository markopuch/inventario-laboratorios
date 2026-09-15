package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(30)
class AuthIntegrationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @LocalServerPort
    private int port;

    private HttpClient client;

    @BeforeEach
    void prepararCliente() {
        this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @AfterEach
    void cerrarCliente() {
        this.client.close();
    }

    @ParameterizedTest
    @CsvSource({ "marko, ADMIN", "aldo, GESTOR", "romel, LECTOR" })
    void autenticaCadaRolYExponeSoloDatosPublicos(String userName, String rol) throws Exception {
        HttpResponse<String> response = login(userName, passwordDemo());
        assertEquals(200, response.statusCode());
        JsonNode auth = this.mapper.readTree(response.body());
        String token = auth.path("accessToken").asString();
        assertFalse(token.isBlank());
        assertEquals("Bearer", auth.path("tokenType").asString());
        assertTrue(auth.path("expiresIn").asLong() > 0);
        assertEquals(userName, auth.path("usuario").path("userName").asString());
        assertEquals(rol, auth.path("usuario").path("rol").asString());
        assertFalse(response.headers().allValues("Set-Cookie").stream()
                .anyMatch(value -> value.contains("JSESSIONID")));
        assertSinContrasena(response.body());

        HttpResponse<String> me = enviar("GET", "/api/auth/me", token, null);
        assertEquals(200, me.statusCode());
        JsonNode usuario = this.mapper.readTree(me.body());
        assertEquals(userName, usuario.path("userName").asString());
        assertEquals(rol, usuario.path("rol").asString());
        assertFalse(usuario.has("accessToken"));
        assertSinContrasena(me.body());
        assertEquals(200, enviar("GET", "/api/categorias", token, null).statusCode());

        String hash = this.jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE username = ?", String.class, userName);
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(this.passwordEncoder.matches(passwordDemo(), hash));
        assertFalse(hash.equals(passwordDemo()));
    }

    @Test
    void credencialesIncorrectasYUsuarioDesconocidoProducenElMismoError() throws Exception {
        HttpResponse<String> incorrecta = login("marko", "clave-incorrecta-" + UUID.randomUUID());
        HttpResponse<String> desconocido = login("inexistente-" + UUID.randomUUID(), passwordDemo());
        assertErrorSeguro(401, incorrecta);
        assertErrorSeguro(401, desconocido);
        assertEquals(this.mapper.readTree(incorrecta.body()).path("message").asString(),
                this.mapper.readTree(desconocido.body()).path("message").asString());
    }

    @Test
    void loginValidaLosCamposObligatorios() throws Exception {
        HttpResponse<String> response = enviar("POST", "/api/auth/login", null, "{}");
        assertErrorSeguro(400, response);
        JsonNode errors = this.mapper.readTree(response.body()).path("errors");
        assertTrue(errors.has("userName"));
        assertTrue(errors.has("password"));
    }

    @Test
    void bcryptNoAceptaSufijosQueExcedenLos72BytesUtf8() throws Exception {
        String passwordLimite = "ñ".repeat(36);
        String hashOriginal = this.jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE username = 'marko'", String.class);
        try {
            this.jdbc.update("UPDATE usuario SET password_hash = ? WHERE username = 'marko'",
                    this.passwordEncoder.encode(passwordLimite));
            assertEquals(200, login("marko", passwordLimite).statusCode());
            assertErrorSeguro(401, login("marko", passwordLimite + "ñ"));
        } finally {
            this.jdbc.update("UPDATE usuario SET password_hash = ? WHERE username = 'marko'", hashOriginal);
        }
    }

    @Test
    void exigeTokenParaConsultarCategoriasYUsuarioActual() throws Exception {
        assertErrorSeguro(401, enviar("GET", "/api/categorias", null, null));
        assertErrorSeguro(401, enviar("GET", "/api/auth/me", null, null));
    }

    @Test
    void rechazaTokensMalformadosAlteradosYVencidos() throws Exception {
        assertErrorSeguro(401, enviar("GET", "/api/categorias", "token-inventado", null));
        String token = tokenDe("marko");
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("marko", "romel");
        String altered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        assertErrorSeguro(401, enviar("GET", "/api/categorias", altered, null));

        Integer id = this.jdbc.queryForObject(
                "SELECT id_usuario FROM usuario WHERE username = 'marko'", Integer.class);
        String expired = Jwts.builder().issuer("inventario-laboratorios").subject("marko").claim("id", id)
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(System.getenv("JWT_SECRET"))),
                        Jwts.SIG.HS256)
                .compact();
        assertErrorSeguro(401, enviar("GET", "/api/categorias", expired, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "aldo", "romel" })
    void gestorYLectorNoPuedenCrearEditarNiEliminarCategorias(String userName) throws Exception {
        String token = tokenDe(userName);
        Integer id = this.jdbc.queryForObject(
                "SELECT MIN(id_categoria) FROM categoria WHERE activo = TRUE", Integer.class);
        assertNotNull(id);
        String json = this.mapper.writeValueAsString(Map.of("nombre", "Intento " + UUID.randomUUID()));
        assertErrorSeguro(403, enviar("POST", "/api/categorias", token, json));
        assertErrorSeguro(403, enviar("PUT", "/api/categorias/" + id, token, json));
        assertErrorSeguro(403, enviar("DELETE", "/api/categorias/" + id, token, null));
        assertEquals(Boolean.TRUE, this.jdbc.queryForObject(
                "SELECT activo FROM categoria WHERE id_categoria = ?", Boolean.class, id));
    }

    @Test
    void administradorPuedeCompletarElCrudConBajaLogica() throws Exception {
        String token = tokenDe("marko");
        String nombre = "JWT admin " + UUID.randomUUID();
        Integer id = null;
        try {
            String json = this.mapper.writeValueAsString(Map.of("nombre", nombre, "descripcion", "Original"));
            HttpResponse<String> created = enviar("POST", "/api/categorias", token, json);
            assertEquals(201, created.statusCode(), created.body());
            JsonNode categoria = this.mapper.readTree(created.body());
            id = categoria.path("id").asInt();
            String path = "/api/categorias/" + id;
            assertTrue(created.headers().firstValue("Location").orElse("").endsWith(path));
            assertTrue(categoria.path("activo").asBoolean());
            assertEquals(200, enviar("GET", path, token, null).statusCode());

            String actualizacion = this.mapper.writeValueAsString(Map.of("nombre", nombre + " editada"));
            HttpResponse<String> updated = enviar("PUT", path, token, actualizacion);
            assertEquals(200, updated.statusCode(), updated.body());
            JsonNode editada = this.mapper.readTree(updated.body());
            assertEquals(id.intValue(), editada.path("id").asInt());
            assertEquals(categoria.path("fechaCreacion"), editada.path("fechaCreacion"));
            assertTrue(editada.path("descripcion").isNull());
            HttpResponse<String> deleted = enviar("DELETE", path, token, null);
            assertEquals(204, deleted.statusCode(), deleted.body());
            assertTrue(deleted.body().isEmpty());
            assertErrorSeguro(404, enviar("GET", path, token, null));
            assertEquals(Boolean.FALSE, this.jdbc.queryForObject(
                    "SELECT activo FROM categoria WHERE id_categoria = ?", Boolean.class, id));
        } finally {
            // Esta clase solo se habilita en bases temporales; borra únicamente su fixture UUID.
            if (id != null) {
                this.jdbc.update("DELETE FROM categoria WHERE id_categoria = ?", id);
            } else {
                this.jdbc.update("DELETE FROM categoria WHERE nombre = ?", nombre);
            }
        }
    }

    @Test
    void elRolEnviadoPorElClienteNoConcedePermisos() throws Exception {
        String json = this.mapper.writeValueAsString(Map.of(
                "userName", "romel", "password", passwordDemo(), "rol", "ADMIN"));
        HttpResponse<String> response = enviar("POST", "/api/auth/login", null, json);
        assertEquals(200, response.statusCode());
        JsonNode auth = this.mapper.readTree(response.body());
        assertEquals("LECTOR", auth.path("usuario").path("rol").asString());
        assertErrorSeguro(403, enviar("POST", "/api/categorias", auth.path("accessToken").asString(),
                this.mapper.writeValueAsString(Map.of("nombre", "Sin permisos " + UUID.randomUUID()))));
    }

    @Test
    void desactivarUsuarioInvalidaSuTokenYSuLoginInmediatamente() throws Exception {
        String token = tokenDe("romel");
        try {
            assertEquals(1, this.jdbc.update("UPDATE usuario SET activo = FALSE WHERE username = 'romel'"));
            assertErrorSeguro(401, enviar("GET", "/api/categorias", token, null));
            assertErrorSeguro(401, login("romel", passwordDemo()));
        } finally {
            this.jdbc.update("UPDATE usuario SET activo = TRUE WHERE username = 'romel'");
        }
        assertEquals(200, enviar("GET", "/api/categorias", token, null).statusCode());
    }

    @Test
    void desactivarRolInvalidaElTokenYElLoginDeSusUsuarios() throws Exception {
        String token = tokenDe("aldo");
        try {
            assertEquals(1, this.jdbc.update("UPDATE rol SET activo = FALSE WHERE nombre = 'GESTOR'"));
            assertErrorSeguro(401, enviar("GET", "/api/categorias", token, null));
            assertErrorSeguro(401, login("aldo", passwordDemo()));
        } finally {
            this.jdbc.update("UPDATE rol SET activo = TRUE WHERE nombre = 'GESTOR'");
        }
        assertEquals(200, enviar("GET", "/api/categorias", token, null).statusCode());
    }

    @Test
    void unCambioDeRolActualizaLosPermisosSinEmitirOtroToken() throws Exception {
        String token = tokenDe("marko");
        Integer rolOriginal = this.jdbc.queryForObject(
                "SELECT id_rol FROM usuario WHERE username = 'marko'", Integer.class);
        try {
            assertEquals(1, this.jdbc.update("""
                    UPDATE usuario SET id_rol = (SELECT id_rol FROM rol WHERE nombre = 'LECTOR')
                    WHERE username = 'marko'
                    """));
            HttpResponse<String> me = enviar("GET", "/api/auth/me", token, null);
            assertEquals(200, me.statusCode());
            assertEquals("LECTOR", this.mapper.readTree(me.body()).path("rol").asString());
            assertEquals(200, enviar("GET", "/api/categorias", token, null).statusCode());
            assertErrorSeguro(403, enviar("POST", "/api/categorias", token,
                    this.mapper.writeValueAsString(Map.of("nombre", "Rol actualizado " + UUID.randomUUID()))));
        } finally {
            this.jdbc.update("UPDATE usuario SET id_rol = ? WHERE username = 'marko'", rolOriginal);
        }
    }

    private String passwordDemo() {
        String password = System.getenv("DEMO_USER_PASSWORD");
        assertNotNull(password, "Configura DEMO_USER_PASSWORD para los usuarios de la base temporal");
        return password;
    }

    private HttpResponse<String> login(String userName, String password) throws Exception {
        return enviar("POST", "/api/auth/login", null,
                this.mapper.writeValueAsString(Map.of("userName", userName, "password", password)));
    }

    private String tokenDe(String userName) throws Exception {
        HttpResponse<String> response = login(userName, passwordDemo());
        assertEquals(200, response.statusCode(), "Debe iniciar sesión el usuario de la prueba: " + userName);
        return this.mapper.readTree(response.body()).path("accessToken").asString();
    }

    private HttpResponse<String> enviar(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path))
                .timeout(TIMEOUT).header("Accept", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            request.header("Content-Type", "application/json");
        }
        request.method(method, json == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));
        return this.client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertErrorSeguro(int status, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        JsonNode error = this.mapper.readTree(response.body());
        assertEquals(status, error.path("status").asInt());
        assertFalse(error.path("message").asString().isBlank());
        assertFalse(error.has("trace"));
        assertFalse(response.body().contains("org.springframework"));
        assertFalse(response.body().contains("io.jsonwebtoken"));
        assertFalse(response.body().contains("password_hash"));
    }

    private void assertSinContrasena(String response) {
        assertFalse(response.contains("password"));
        assertFalse(response.contains("passwordHash"));
        assertFalse(response.contains("password_hash"));
        assertFalse(response.contains("$2a$"));
        assertFalse(response.contains("$2b$"));
    }
}
