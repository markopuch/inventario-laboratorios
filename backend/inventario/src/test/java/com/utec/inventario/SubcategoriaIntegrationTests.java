package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

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
@Timeout(30)
class SubcategoriaIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @LocalServerPort private int port;

    private HttpClient client;
    private String token;
    private Integer categoriaA;
    private Integer categoriaB;

    @BeforeEach
    void preparar() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.token = tokenDe("marko");
        this.categoriaA = nuevaCategoria("A");
        this.categoriaB = nuevaCategoria("B");
    }

    @AfterEach
    void limpiarSoloFixturesDeLaBaseTemporal() {
        this.client.close();
        this.jdbc.update("DELETE FROM subcategoria WHERE id_categoria IN (?, ?)", categoriaA, categoriaB);
        this.jdbc.update("DELETE FROM categoria WHERE id_categoria IN (?, ?)", categoriaA, categoriaB);
    }

    @Test
    void adminCompletaCrudConRelacionFechaGeneradaYPutCompleto() throws Exception {
        HttpResponse<String> created = enviar("POST", "/api/subcategorias", token,
                json("  Sensores  ", "  Descripción  ", categoriaA));
        assertEquals(201, created.statusCode(), created.body());
        JsonNode original = mapper.readTree(created.body());
        int id = original.path("id").asInt();
        String path = "/api/subcategorias/" + id;
        assertTrue(created.headers().firstValue("Location").orElse("").endsWith(path));
        assertEquals("Sensores", original.path("nombre").asString());
        assertEquals("Descripción", original.path("descripcion").asString());
        assertTrue(original.path("activo").asBoolean());
        assertFalse(original.path("fechaCreacion").asString().isBlank());
        assertEquals(categoriaA.intValue(), original.path("categoria").path("id").asInt());
        assertEquals(2, original.path("categoria").size(), "El resumen solo contiene id y nombre");
        assertEquals(200, enviar("GET", path, token, null).statusCode());

        HttpResponse<String> updated = enviar("PUT", path, token, json("Sensores", null, categoriaA));
        assertEquals(200, updated.statusCode(), updated.body());
        JsonNode change = mapper.readTree(updated.body());
        assertEquals(original.path("fechaCreacion"), change.path("fechaCreacion"));
        assertTrue(change.path("descripcion").isNull(), "PUT omitida debe limpiar descripción");
        assertEquals(id, change.path("id").asInt());
        assertTrue(change.path("activo").asBoolean());

        HttpResponse<String> deleted = enviar("DELETE", path, token, null);
        assertEquals(204, deleted.statusCode(), deleted.body());
        assertTrue(deleted.body().isEmpty());
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "SELECT activo FROM subcategoria WHERE id_subcategoria = ?", Boolean.class, id));
        assertError(404, enviar("GET", path, token, null));
        assertError(404, enviar("PUT", path, token, json("Otro", null, categoriaA)));
        assertError(404, enviar("DELETE", path, token, null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT" })
    void padreInexistenteProduce404SinGuardar(String method) throws Exception {
        int id = crear("Original", categoriaA);
        int missing = Integer.MAX_VALUE;
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM categoria WHERE id_categoria = ?",
                Integer.class, missing));
        assertError(404, enviar(method, "/api/subcategorias" + (method.equals("PUT") ? "/" + id : ""),
                token, json("Ausente", null, missing)));
        assertEquals(categoriaA, jdbc.queryForObject(
                "SELECT id_categoria FROM subcategoria WHERE id_subcategoria = ?", Integer.class, id));
        assertEquals(1, contarHijas());
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT" })
    void padreInactivoProduce409SinCrearNiMover(String method) throws Exception {
        int id = crear("Original", categoriaA);
        assertEquals(204, enviar("DELETE", "/api/categorias/" + categoriaB, token, null).statusCode());
        assertError(409, enviar(method, "/api/subcategorias" + (method.equals("PUT") ? "/" + id : ""),
                token, json("No guardar", null, categoriaB)));
        assertEquals(categoriaA, jdbc.queryForObject(
                "SELECT id_categoria FROM subcategoria WHERE id_subcategoria = ?", Integer.class, id));
        assertEquals(1, contarHijas());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Sensores", "sensores", "SENSORES" })
    void duplicadoMismaCategoriaSeRechazaInclusoInactivo(String nombre) throws Exception {
        int id = crear("Sensores", categoriaA);
        assertError(409, enviar("POST", "/api/subcategorias", token, json(nombre, null, categoriaA)));
        assertEquals(204, enviar("DELETE", "/api/subcategorias/" + id, token, null).statusCode());
        assertError(409, enviar("POST", "/api/subcategorias", token, json(nombre, null, categoriaA)));
        assertEquals(1, contarHijas());
    }

    @Test
    void mismoNombreEnOtroPadreEsValidoYMovimientoRevisaDuplicadoDestino() throws Exception {
        int id = crear("Sensores", categoriaA);
        crear("sensores", categoriaB);
        assertError(409, enviar("PUT", "/api/subcategorias/" + id, token, json("SENSORES", null, categoriaB)));
        assertEquals(categoriaA, jdbc.queryForObject(
                "SELECT id_categoria FROM subcategoria WHERE id_subcategoria = ?", Integer.class, id));
        HttpResponse<String> moved = enviar("PUT", "/api/subcategorias/" + id, token,
                json("Otro nombre", null, categoriaB));
        assertEquals(200, moved.statusCode(), moved.body());
        assertEquals(categoriaB.intValue(), mapper.readTree(moved.body()).path("categoria").path("id").asInt());
        assertEquals(categoriaB, jdbc.queryForObject(
                "SELECT id_categoria FROM subcategoria WHERE id_subcategoria = ?", Integer.class, id));
        assertEquals(0, mapper.readTree(enviar("GET", rutaHijas(categoriaA), token, null).body()).size());
        assertEquals(2, mapper.readTree(enviar("GET", rutaHijas(categoriaB), token, null).body()).size());
    }

    @Test
    void consultasFiltranActivasYPadreDeLaRutaJerarquica() throws Exception {
        int active = crear("Activa", categoriaA);
        int inactive = crear("Inactiva", categoriaA);
        crear("Otro padre", categoriaB);
        assertEquals(204, enviar("DELETE", "/api/subcategorias/" + inactive, token, null).statusCode());
        JsonNode all = mapper.readTree(enviar("GET", "/api/subcategorias", token, null).body());
        boolean found = false;
        for (JsonNode row : all) {
            assertTrue(row.path("activo").asBoolean());
            assertNotEquals(inactive, row.path("id").asInt());
            found |= row.path("id").asInt() == active;
        }
        assertTrue(found);
        HttpResponse<String> response = enviar("GET", rutaHijas(categoriaA), token, null);
        assertEquals(200, response.statusCode());
        JsonNode children = mapper.readTree(response.body());
        assertEquals(1, children.size());
        assertEquals(active, children.get(0).path("id").asInt());
        assertError(404, enviar("GET", rutaHijas(Integer.MAX_VALUE), token, null));
        assertEquals(204, enviar("DELETE", "/api/subcategorias/" + active, token, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/categorias/" + categoriaA, token, null).statusCode());
        assertError(404, enviar("GET", rutaHijas(categoriaA), token, null));
    }

    @Test
    void categoriaConHijaActivaPermaneceActivaYConHijaInactivaPermiteBaja() throws Exception {
        int id = crear("Hija", categoriaA);
        assertError(409, enviar("DELETE", "/api/categorias/" + categoriaA, token, null));
        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "SELECT activo FROM categoria WHERE id_categoria = ?", Boolean.class, categoriaA));
        assertEquals(204, enviar("DELETE", "/api/subcategorias/" + id, token, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/categorias/" + categoriaA, token, null).statusCode());
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "SELECT activo FROM categoria WHERE id_categoria = ?", Boolean.class, categoriaA));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"nombre\":\"\",\"idCategoria\":1}",
        "{\"nombre\":\"   \",\"idCategoria\":1}",
        "{\"nombre\":\"Nombre\"}", "{\"nombre\":\"Nombre\",\"idCategoria\":null}",
        "{\"nombre\":\"Nombre\",\"idCategoria\":0}",
        "{\"nombre\":\"Nombre\",\"idCategoria\":-1}",
        "{\"nombre\":\"Nombre\",\"idCategoria\":\"abc\"}", "{"
    })
    void validaJsonYCamposObligatoriosEnPostYPut(String invalid) throws Exception {
        int id = crear("Para validar", categoriaA);
        assertError(400, enviar("POST", "/api/subcategorias", token, invalid));
        assertError(400, enviar("PUT", "/api/subcategorias/" + id, token, invalid));
        assertEquals(1, contarHijas());
    }

    @Test
    void validaLongitudesEIdDeRuta() throws Exception {
        assertError(400, enviar("POST", "/api/subcategorias", token, json("a".repeat(101), null, categoriaA)));
        assertError(400, enviar("POST", "/api/subcategorias", token, json("Nombre", "a".repeat(256), categoriaA)));
        assertError(400, enviar("GET", "/api/subcategorias/abc", token, null));
        assertError(400, enviar("GET", "/api/subcategorias/0", token, null));
        assertError(400, enviar("GET", rutaHijas(-1), token, null));
        assertError(404, enviar("GET", "/api/subcategorias/2147483647", token, null));
        assertEquals(0, contarHijas());
    }

    @ParameterizedTest
    @ValueSource(strings = { "marko", "aldo", "romel" })
    void tresRolesPuedenLeerTodasLasRutas(String userName) throws Exception {
        int id = crear("Lectura", categoriaA);
        String reader = tokenDe(userName);
        assertEquals(200, enviar("GET", "/api/subcategorias", reader, null).statusCode());
        assertEquals(200, enviar("GET", "/api/subcategorias/" + id, reader, null).statusCode());
        assertEquals(200, enviar("GET", rutaHijas(categoriaA), reader, null).statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = { "aldo", "romel" })
    void gestorYLectorNoPuedenEscribir(String userName) throws Exception {
        int id = crear("Protegida", categoriaA);
        String reader = tokenDe(userName);
        assertError(403, enviar("POST", "/api/subcategorias", reader, json("Otra", null, categoriaA)));
        assertError(403, enviar("PUT", "/api/subcategorias/" + id, reader, json("Cambio", null, categoriaB)));
        assertError(403, enviar("DELETE", "/api/subcategorias/" + id, reader, null));
        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "SELECT activo FROM subcategoria WHERE id_subcategoria = ?", Boolean.class, id));
        assertEquals(1, contarHijas());
    }

    @Test
    void sinTokenOTokenInvalidoNoPuedeAcceder() throws Exception {
        int id = crear("Privada", categoriaA);
        for (String credential : new String[] { null, "token-invalido" }) {
            assertError(401, enviar("GET", "/api/subcategorias", credential, null));
            assertError(401, enviar("GET", "/api/subcategorias/" + id, credential, null));
            assertError(401, enviar("GET", rutaHijas(categoriaA), credential, null));
            assertError(401, enviar("POST", "/api/subcategorias", credential, json("Intento", null, categoriaA)));
            assertError(401, enviar("PUT", "/api/subcategorias/" + id, credential, json("Intento", null, categoriaA)));
            assertError(401, enviar("DELETE", "/api/subcategorias/" + id, credential, null));
        }
    }

    @Test
    void flywayAplicoV7YElIndiceProtegeTambienInsercionesDirectas() {
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '7' AND success", Integer.class));
        jdbc.update("INSERT INTO subcategoria (nombre, id_categoria, activo) VALUES (?, ?, FALSE)",
                "Reserva SQL", categoriaA);
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO subcategoria (nombre, id_categoria) VALUES (?, ?)", "reserva sql", categoriaA));
        assertEquals(1, jdbc.update("INSERT INTO subcategoria (nombre, id_categoria) VALUES (?, ?)",
                "RESERVA SQL", categoriaB));
    }

    private Integer nuevaCategoria(String label) {
        return jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria",
                Integer.class, "S4A " + label + " " + UUID.randomUUID());
    }

    private int crear(String nombre, Integer categoria) throws Exception {
        HttpResponse<String> response = enviar("POST", "/api/subcategorias", token, json(nombre, null, categoria));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body()).path("id").asInt();
    }

    private int contarHijas() {
        return jdbc.queryForObject("SELECT count(*) FROM subcategoria WHERE id_categoria IN (?, ?)",
                Integer.class, categoriaA, categoriaB);
    }

    private String json(String nombre, String descripcion, Integer categoria) {
        if (descripcion == null) {
            return mapper.writeValueAsString(Map.of("nombre", nombre, "idCategoria", categoria));
        }
        return mapper.writeValueAsString(Map.of("nombre", nombre, "descripcion", descripcion, "idCategoria", categoria));
    }

    private String rutaHijas(Integer categoria) { return "/api/categorias/" + categoria + "/subcategorias"; }

    private String tokenDe(String username) {
        return jwtService.generateToken((UserInfoDetails) usuarioService.loadUserByUsername(username));
    }

    private HttpResponse<String> enviar(String method, String path, String bearer, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
        if (bearer != null) request.header("Authorization", "Bearer " + bearer);
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertError(int expected, HttpResponse<String> response) {
        assertEquals(expected, response.statusCode(), response.body());
        JsonNode error = mapper.readTree(response.body());
        assertEquals(expected, error.path("status").asInt());
        assertFalse(error.path("message").asString().isBlank());
        assertFalse(error.has("trace"));
        assertFalse(response.body().contains("org.hibernate"));
        assertFalse(response.body().contains("uq_subcategoria"));
        assertFalse(response.body().contains("password_hash"));
    }
}
