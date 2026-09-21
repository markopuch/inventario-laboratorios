package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DuplicateKeyException;
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
class OrganizacionIntegrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @LocalServerPort private int port;

    private HttpClient client;
    private String token;
    private String run;
    private int sedeA;
    private int sedeB;
    private final List<Integer> sedesPropias = new ArrayList<>();

    @BeforeEach
    void preparar() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        token = tokenDe("marko");
        run = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        sedeA = sedeFixture("A");
        sedeB = sedeFixture("B");
    }

    @AfterEach
    void limpiarExclusivamenteFixturesDeLaBaseDeVerificacion() {
        if (client != null) client.close();
        for (Integer id : sedesPropias) {
            jdbc.update("DELETE FROM laboratorio WHERE id_area IN (SELECT id_area FROM area WHERE id_sede = ?)", id);
            jdbc.update("DELETE FROM area WHERE id_sede = ?", id);
            jdbc.update("DELETE FROM sede WHERE id_sede = ?", id);
        }
    }

    @Test
    void sedeCrudNormalizaPermiteNombresRepetidosYConservaLaBajaLogica() throws Exception {
        String name = "Sede HTTP " + run;
        String body = json("nombre", "  " + name + "  ", "direccion", "  Avenida  ",
                "distrito", "  Barranco  ", "departamento", "  Lima  ", "id", 999999,
                "activo", false, "fechaCreacion", "2000-01-01T00:00:00Z");
        HttpResponse<String> created = enviar("POST", "/api/sedes", token, body);
        JsonNode original = comprobarCreacion(created, "/api/sedes");
        int id = original.path("id").asInt();
        sedesPropias.add(id);
        assertEquals(name, original.path("nombre").asString());
        assertEquals("Avenida", original.path("direccion").asString());
        assertEquals("Barranco", original.path("distrito").asString());
        assertEquals("Lima", original.path("departamento").asString());
        assertNotEquals(999999, id);
        JsonNode repeated = comprobarCreacion(enviar("POST", "/api/sedes", token, json("nombre", name)), "/api/sedes");
        sedesPropias.add(repeated.path("id").asInt());
        assertNotEquals(id, repeated.path("id").asInt(), "Sede no tiene unicidad de nombre");
        String path = "/api/sedes/" + id;
        assertEquals(original, obtener(path));
        contieneActivo("/api/sedes", id);
        JsonNode updated = correcto(200, enviar("PUT", path, token,
                json("nombre", "  Sede actualizada  ", "id", 999999, "activo", false,
                        "fechaCreacion", "2000-01-01T00:00:00Z")));
        comprobarIdentidadYFecha(original, updated);
        assertEquals("Sede actualizada", updated.path("nombre").asString());
        for (String field : List.of("direccion", "distrito", "departamento")) assertTrue(updated.path(field).isNull());
        comprobarBaja("sede", "id_sede", path, id, json("nombre", "No revivir"));
        noContiene("/api/sedes", id);
    }

    @Test
    void areaCrudMantieneNombrePropioPermiteMoverYLimpiaDescripcionEnPut() throws Exception {
        HttpResponse<String> created = enviar("POST", "/api/areas", token,
                json("nombre", "  Mecatrónica  ", "descripcion", "  Curso  ", "idSede", sedeA,
                        "activo", false, "id", 999999, "fechaCreacion", "2000-01-01T00:00:00Z"));
        JsonNode original = comprobarCreacion(created, "/api/areas");
        int id = original.path("id").asInt();
        String path = "/api/areas/" + id;
        assertEquals("Mecatrónica", original.path("nombre").asString());
        assertEquals("Curso", original.path("descripcion").asString());
        assertEquals(sedeA, original.path("sede").path("id").asInt());
        assertEquals(2, original.path("sede").size());
        assertEquals(original, obtener(path));
        contieneActivo("/api/areas", id);
        contieneActivo(areasDe(sedeA), id);
        JsonNode updated = correcto(200, enviar("PUT", path, token, areaJson("Mecatrónica", sedeA)));
        comprobarIdentidadYFecha(original, updated);
        assertTrue(updated.path("descripcion").isNull());
        JsonNode moved = correcto(200, enviar("PUT", path, token, areaJson("Mecatrónica", sedeB)));
        comprobarIdentidadYFecha(original, moved);
        assertEquals(sedeB, moved.path("sede").path("id").asInt());
        assertEquals(sedeB, jdbc.queryForObject("SELECT id_sede FROM area WHERE id_area = ?", Integer.class, id));
        noContiene(areasDe(sedeA), id);
        contieneActivo(areasDe(sedeB), id);
        comprobarBaja("area", "id_area", path, id, areaJson("No revivir", sedeB));
        noContiene("/api/areas", id);
        noContiene(areasDe(sedeB), id);
    }

    @Test
    void laboratorioCrudMantieneCodigoPropioPermiteMoverYLimpiaUbicacion() throws Exception {
        int areaA = areaFixture(sedeA, "Origen");
        int areaB = areaFixture(sedeB, "Destino");
        String code = codigo("C");
        JsonNode original = comprobarCreacion(enviar("POST", "/api/laboratorios", token,
                json("nombre", "  Robótica  ", "codigo", "  " + code + "  ", "ubicacion", "  Piso 5  ",
                        "idArea", areaA, "activo", false, "id", 999999, "fechaCreacion", "2000-01-01T00:00:00Z")),
                "/api/laboratorios");
        int id = original.path("id").asInt();
        String path = "/api/laboratorios/" + id;
        assertEquals("Robótica", original.path("nombre").asString());
        assertEquals(code, original.path("codigo").asString());
        assertEquals("Piso 5", original.path("ubicacion").asString());
        assertEquals(areaA, original.path("area").path("id").asInt());
        assertEquals(2, original.path("area").size());
        assertEquals(original, obtener(path));
        contieneActivo("/api/laboratorios", id);
        contieneActivo(laboratoriosDe(areaA), id);
        JsonNode updated = correcto(200, enviar("PUT", path, token, labJson(code, areaA)));
        comprobarIdentidadYFecha(original, updated);
        assertTrue(updated.path("ubicacion").isNull());
        JsonNode moved = correcto(200, enviar("PUT", path, token, labJson(code, areaB)));
        comprobarIdentidadYFecha(original, moved);
        assertEquals(areaB, moved.path("area").path("id").asInt());
        assertEquals(areaB, jdbc.queryForObject("SELECT id_area FROM laboratorio WHERE id_laboratorio = ?", Integer.class, id));
        noContiene(laboratoriosDe(areaA), id);
        contieneActivo(laboratoriosDe(areaB), id);
        comprobarBaja("laboratorio", "id_laboratorio", path, id, labJson(code, areaB));
        noContiene("/api/laboratorios", id);
        noContiene(laboratoriosDe(areaB), id);
    }

    @Test
    void areaNombreEsUnicoPorSedeIncluyendoInactivasYMovimientoCompruebaDestino() throws Exception {
        int id = crearArea("Mecatrónica", sedeA);
        for (String name : List.of("Mecatrónica", "mecatrónica")) {
            assertError(409, enviar("POST", "/api/areas", token, areaJson(name, sedeA)));
        }
        int other = crearArea("mecatrónica", sedeB);
        assertError(409, enviar("PUT", "/api/areas/" + id, token, areaJson("MECATRÓNICA", sedeB)));
        assertEquals(sedeA, jdbc.queryForObject("SELECT id_sede FROM area WHERE id_area = ?", Integer.class, id));
        assertEquals(204, enviar("DELETE", "/api/areas/" + other, token, null).statusCode());
        assertError(409, enviar("POST", "/api/areas", token, areaJson("MECATRÓNICA", sedeB)));
        assertError(409, enviar("PUT", "/api/areas/" + id, token, areaJson("Mecatrónica", sedeB)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM area WHERE id_sede = ?", Integer.class, sedeB));
    }

    @Test
    void laboratorioCodigoEsUnicoGlobalInclusoEnOtraAreaYTrasLaBaja() throws Exception {
        int areaA = areaFixture(sedeA, "A");
        int areaB = areaFixture(sedeB, "B");
        String code = codigo("UNICO");
        int id = crearLaboratorio(code, areaA);
        int other = crearLaboratorio(codigo("OTRO"), areaB);
        for (int parent : new int[] { areaA, areaB }) {
            for (String duplicate : List.of(code, code.toLowerCase(Locale.ROOT))) {
                assertError(409, enviar("POST", "/api/laboratorios", token, labJson(duplicate, parent)));
            }
        }
        assertError(409, enviar("PUT", "/api/laboratorios/" + other, token, labJson(code, areaB)));
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + id, token, null).statusCode());
        assertError(409, enviar("POST", "/api/laboratorios", token, labJson(code.toLowerCase(Locale.ROOT), areaB)));
        assertError(409, enviar("PUT", "/api/laboratorios/" + other, token, labJson(code, areaB)));
        assertEquals(codigo("OTRO"), obtener("/api/laboratorios/" + other).path("codigo").asString());
    }

    @Test
    void areaExigeSedeExistenteYActivaAlCrearYMover() throws Exception {
        int id = crearArea("Original", sedeA);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM sede WHERE id_sede = ?", Integer.class, Integer.MAX_VALUE));
        assertEquals(204, enviar("DELETE", "/api/sedes/" + sedeB, token, null).statusCode());
        for (String method : List.of("POST", "PUT")) {
            String path = "/api/areas" + (method.equals("PUT") ? "/" + id : "");
            assertError(404, enviar(method, path, token, areaJson("Ausente", Integer.MAX_VALUE)));
            assertError(409, enviar(method, path, token, areaJson("Inactiva", sedeB)));
        }
        assertEquals(sedeA, obtener("/api/areas/" + id).path("sede").path("id").asInt());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM area WHERE id_sede IN (?, ?)", Integer.class, sedeA, sedeB));
    }

    @Test
    void laboratorioExigeAreaExistenteYActivaAlCrearYMover() throws Exception {
        int areaA = areaFixture(sedeA, "A");
        int areaB = areaFixture(sedeB, "B");
        int id = crearLaboratorio(codigo("PADRE"), areaA);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM area WHERE id_area = ?", Integer.class, Integer.MAX_VALUE));
        assertEquals(204, enviar("DELETE", "/api/areas/" + areaB, token, null).statusCode());
        for (String method : List.of("POST", "PUT")) {
            String path = "/api/laboratorios" + (method.equals("PUT") ? "/" + id : "");
            assertError(404, enviar(method, path, token, labJson(codigo("AUSENTE"), Integer.MAX_VALUE)));
            assertError(409, enviar(method, path, token, labJson(codigo("INACTIVO"), areaB)));
        }
        assertEquals(areaA, obtener("/api/laboratorios/" + id).path("area").path("id").asInt());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM laboratorio WHERE id_area IN (?, ?)", Integer.class, areaA, areaB));
    }

    @Test
    void padresConHijosActivosNoSeDesactivanYBajaDeHijosLiberaLaJerarquia() throws Exception {
        int area = crearArea("Con hija", sedeA);
        int lab = crearLaboratorio(codigo("HIJO"), area);
        assertError(409, enviar("DELETE", "/api/sedes/" + sedeA, token, null));
        assertError(409, enviar("DELETE", "/api/areas/" + area, token, null));
        assertTrue(obtener("/api/sedes/" + sedeA).path("activo").asBoolean());
        assertTrue(obtener("/api/areas/" + area).path("activo").asBoolean());
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + lab, token, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/areas/" + area, token, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/sedes/" + sedeA, token, null).statusCode());
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM sede WHERE id_sede = ?", Boolean.class, sedeA));
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM area WHERE id_area = ?", Boolean.class, area));
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM laboratorio WHERE id_laboratorio = ?", Boolean.class, lab));
    }

    @Test
    void rutasJerarquicasDistinguenPadresActivosVaciosDeInactivosOAusentes() throws Exception {
        assertEquals(0, obtener(areasDe(sedeA)).size());
        int area = crearArea("Vacía", sedeA);
        assertEquals(0, obtener(laboratoriosDe(area)).size());
        assertError(404, enviar("GET", areasDe(Integer.MAX_VALUE), token, null));
        assertError(404, enviar("GET", laboratoriosDe(Integer.MAX_VALUE), token, null));
        assertEquals(204, enviar("DELETE", "/api/areas/" + area, token, null).statusCode());
        assertEquals(0, obtener(areasDe(sedeA)).size());
        assertError(404, enviar("GET", laboratoriosDe(area), token, null));
        assertEquals(204, enviar("DELETE", "/api/sedes/" + sedeA, token, null).statusCode());
        assertError(404, enviar("GET", areasDe(sedeA), token, null));
    }

    @Test
    void gestorYLectorLeenTodasLasRutasPeroNoPuedenEscribir() throws Exception {
        int area = crearArea("Permisos", sedeA);
        int lab = crearLaboratorio(codigo("PERMISOS"), area);
        String[] bases = { "/api/sedes", "/api/areas", "/api/laboratorios" };
        int[] ids = { sedeA, area, lab };
        String[] bodies = { json("nombre", "Intento"), areaJson("Intento", sedeA), labJson(codigo("INTENTO"), area) };
        for (String user : List.of("aldo", "romel")) {
            String reader = tokenDe(user);
            for (int i = 0; i < bases.length; i++) {
                correcto(200, enviar("GET", bases[i], reader, null));
                correcto(200, enviar("GET", bases[i] + "/" + ids[i], reader, null));
                assertError(403, enviar("POST", bases[i], reader, bodies[i]));
                assertError(403, enviar("PUT", bases[i] + "/" + ids[i], reader, bodies[i]));
                assertError(403, enviar("DELETE", bases[i] + "/" + ids[i], reader, null));
            }
            correcto(200, enviar("GET", areasDe(sedeA), reader, null));
            correcto(200, enviar("GET", laboratoriosDe(area), reader, null));
        }
        assertEquals("Permisos", obtener("/api/areas/" + area).path("nombre").asString());
        assertTrue(obtener("/api/laboratorios/" + lab).path("activo").asBoolean());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM area WHERE id_sede = ?", Integer.class, sedeA));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM laboratorio WHERE id_area = ?", Integer.class, area));
    }

    @Test
    void sinJwtOJwtInvalidoSeRechazanTodasLasRutasAntesDelController() throws Exception {
        int area = crearArea("Privada", sedeA);
        int lab = crearLaboratorio(codigo("PRIVADO"), area);
        String[] bases = { "/api/sedes", "/api/areas", "/api/laboratorios" };
        int[] ids = { sedeA, area, lab };
        String[] bodies = { json("nombre", "Intento"), areaJson("Intento", sedeA), labJson(codigo("INTENTO"), area) };
        for (String credential : new String[] { null, "token-invalido" }) {
            for (int i = 0; i < bases.length; i++) {
                assertError(401, enviar("GET", bases[i], credential, null));
                assertError(401, enviar("GET", bases[i] + "/" + ids[i], credential, null));
                assertError(401, enviar("POST", bases[i], credential, bodies[i]));
                assertError(401, enviar("PUT", bases[i] + "/" + ids[i], credential, bodies[i]));
                assertError(401, enviar("DELETE", bases[i] + "/" + ids[i], credential, null));
            }
            assertError(401, enviar("GET", areasDe(sedeA), credential, null));
            assertError(401, enviar("GET", laboratoriosDe(area), credential, null));
        }
    }

    @Test
    void validacionRechazaBlancosLongitudesIdsYJsonEnPostYPut() throws Exception {
        int area = crearArea("Validación", sedeA);
        int lab = crearLaboratorio(codigo("VALIDO"), area);
        Map<String, Integer> resources = Map.of("/api/sedes", sedeA, "/api/areas", area, "/api/laboratorios", lab);
        for (var resource : resources.entrySet()) {
            for (String invalid : List.of("{", "{}", "{\"nombre\":\"   \"}")) {
                for (String method : List.of("POST", "PUT")) {
                    assertError(400, enviar(method, resource.getKey() + (method.equals("PUT") ? "/" + resource.getValue() : ""), token, invalid));
                }
            }
        }
        List<String> invalidSedes = List.of(json("nombre", "x".repeat(101)),
                json("nombre", "Nombre", "direccion", "x".repeat(201)),
                json("nombre", "Nombre", "distrito", "x".repeat(101)),
                json("nombre", "Nombre", "departamento", "x".repeat(101)));
        List<String> invalidAreas = new ArrayList<>(List.of(areaJson("x".repeat(101), sedeA),
                json("nombre", "Área", "descripcion", "x".repeat(256), "idSede", sedeA)));
        List<String> invalidLabs = new ArrayList<>(List.of(
                json("nombre", "x".repeat(101), "codigo", codigo("LONG"), "idArea", area),
                labJson("x".repeat(31), area), labJson("   ", area),
                json("nombre", "Lab", "codigo", codigo("LONG"), "ubicacion", "x".repeat(201), "idArea", area)));
        for (Object invalidId : new Object[] { null, 0, -1, "abc" }) {
            invalidAreas.add(json("nombre", "Área", "idSede", invalidId));
            invalidLabs.add(json("nombre", "Lab", "codigo", codigo("INVALID"), "idArea", invalidId));
        }
        invalidAreas.add(json("nombre", "Área"));
        invalidLabs.add(json("nombre", "Lab", "codigo", codigo("INVALID")));
        invalidLabs.add(json("nombre", "Lab", "idArea", area));
        validarCuerpos("/api/sedes", sedeA, invalidSedes);
        validarCuerpos("/api/areas", area, invalidAreas);
        validarCuerpos("/api/laboratorios", lab, invalidLabs);
        assertEquals("Validación", obtener("/api/areas/" + area).path("nombre").asString());
        assertEquals(codigo("VALIDO"), obtener("/api/laboratorios/" + lab).path("codigo").asString());
    }

    @Test
    void idsDeRutaInvalidosDan400YAusentesDan404() throws Exception {
        for (String base : List.of("/api/sedes", "/api/areas", "/api/laboratorios")) {
            for (String invalid : List.of("abc", "0", "-1", "2147483648")) {
                assertError(400, enviar("GET", base + "/" + invalid, token, null));
                assertError(400, enviar("DELETE", base + "/" + invalid, token, null));
            }
            assertError(404, enviar("GET", base + "/2147483647", token, null));
        }
        for (String invalid : List.of("abc", "0", "-1")) {
            assertError(400, enviar("GET", "/api/sedes/" + invalid + "/areas", token, null));
            assertError(400, enviar("GET", "/api/areas/" + invalid + "/laboratorios", token, null));
        }
    }

    @Test
    void indicesDeBdReservanNombresYCodigosInactivosConAlcanceCorrecto() {
        assertEquals(2, jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version IN ('8', '9') AND success", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' "
                + "AND indexname IN ('uq_area_sede_nombre_ignore_case', 'uq_laboratorio_codigo_ignore_case')", Integer.class));
        jdbc.update("INSERT INTO area(nombre, id_sede, activo) VALUES (?, ?, FALSE)", "Reserva SQL", sedeA);
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO area(nombre, id_sede) VALUES (?, ?)", "reserva sql", sedeA));
        int areaA = areaFixture(sedeA, "SQL A");
        int areaB = areaFixture(sedeB, "Reserva SQL");
        jdbc.update("INSERT INTO laboratorio(nombre, codigo, id_area, activo) VALUES (?, ?, ?, FALSE)",
                "Reserva SQL", codigo("SQL"), areaA);
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO laboratorio(nombre, codigo, id_area) VALUES (?, ?, ?)",
                "En otra área", codigo("SQL").toLowerCase(Locale.ROOT), areaB));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM area WHERE id_sede = ?", Integer.class, sedeB));
    }

    private void validarCuerpos(String base, int id, List<String> bodies) throws Exception {
        for (String body : bodies) {
            assertError(400, enviar("POST", base, token, body));
            assertError(400, enviar("PUT", base + "/" + id, token, body));
        }
    }

    private int sedeFixture(String name) {
        int id = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede", Integer.class,
                "S4BD " + name + " " + run);
        sedesPropias.add(id);
        return id;
    }

    private int areaFixture(int sede, String name) {
        return jdbc.queryForObject("INSERT INTO area(nombre, id_sede) VALUES (?, ?) RETURNING id_area",
                Integer.class, name, sede);
    }

    private int crearArea(String name, int sede) throws Exception {
        return comprobarCreacion(enviar("POST", "/api/areas", token, areaJson(name, sede)), "/api/areas").path("id").asInt();
    }

    private int crearLaboratorio(String code, int area) throws Exception {
        return comprobarCreacion(enviar("POST", "/api/laboratorios", token, labJson(code, area)), "/api/laboratorios").path("id").asInt();
    }

    private String areaJson(String name, int sede) { return json("nombre", name, "idSede", sede); }
    private String labJson(String code, int area) { return json("nombre", "Laboratorio", "codigo", code, "idArea", area); }
    private String codigo(String label) { return label + "-" + run; }
    private String areasDe(int sede) { return "/api/sedes/" + sede + "/areas"; }
    private String laboratoriosDe(int area) { return "/api/areas/" + area + "/laboratorios"; }

    private String json(Object... entries) {
        Map<String, Object> object = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) object.put((String) entries[i], entries[i + 1]);
        return mapper.writeValueAsString(object);
    }

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

    private JsonNode obtener(String path) throws Exception { return correcto(200, enviar("GET", path, token, null)); }

    private JsonNode correcto(int status, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    private JsonNode comprobarCreacion(HttpResponse<String> response, String base) {
        JsonNode body = correcto(201, response);
        assertTrue(body.path("id").asInt() > 0);
        assertTrue(body.path("activo").asBoolean());
        assertFalse(body.path("fechaCreacion").asString().isBlank());
        assertFalse(body.path("fechaCreacion").asString().startsWith("2000-01-01"));
        assertTrue(response.headers().firstValue("Location").orElse("").endsWith(base + "/" + body.path("id").asInt()));
        return body;
    }

    private void comprobarIdentidadYFecha(JsonNode original, JsonNode updated) {
        assertEquals(original.path("id"), updated.path("id"));
        assertEquals(original.path("fechaCreacion"), updated.path("fechaCreacion"));
        assertTrue(updated.path("activo").asBoolean());
    }

    private void contieneActivo(String path, int id) throws Exception {
        boolean found = false;
        JsonNode rows = obtener(path);
        assertTrue(rows.isArray());
        for (JsonNode row : rows) {
            assertTrue(row.path("activo").asBoolean());
            found |= row.path("id").asInt() == id;
        }
        assertTrue(found, "Debe incluir el recurso activo " + id + " en " + path);
    }

    private void noContiene(String path, int id) throws Exception {
        for (JsonNode row : obtener(path)) assertNotEquals(id, row.path("id").asInt());
    }

    private void comprobarBaja(String table, String idColumn, String path, int id, String validUpdate) throws Exception {
        HttpResponse<String> response = enviar("DELETE", path, token, null);
        assertEquals(204, response.statusCode(), response.body());
        assertTrue(response.body().isEmpty());
        // Los nombres de tabla/columna son constantes de este test, nunca entrada HTTP.
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM " + table + " WHERE " + idColumn + " = ?", Boolean.class, id));
        assertError(404, enviar("GET", path, token, null));
        assertError(404, enviar("PUT", path, token, validUpdate));
        assertError(404, enviar("DELETE", path, token, null));
    }

    private void assertError(int expected, HttpResponse<String> response) {
        JsonNode error = correcto(expected, response);
        assertEquals(expected, error.path("status").asInt());
        assertFalse(error.path("message").asString().isBlank());
        assertFalse(error.has("trace"));
        for (String privateDetail : List.of("org.hibernate", "password_hash", "uq_area", "uq_laboratorio", "23505")) {
            assertFalse(response.body().contains(privateDetail), "El error no debe exponer detalles de persistencia");
        }
    }
}
