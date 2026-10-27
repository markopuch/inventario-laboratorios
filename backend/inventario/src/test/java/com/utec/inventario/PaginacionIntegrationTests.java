package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
class PaginacionIntegrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarios;
    @LocalServerPort private int port;
    private HttpClient client;
    private String run;
    private int sede, area, categoria, subcategoria, labA, labB, adminId;
    private String admin, gestor, lector, vacio;
    private final List<Integer> users = new ArrayList<>();
    private final List<Integer> equipment = new ArrayList<>();
    private final List<Integer> audit = new ArrayList<>();

    @BeforeEach
    void prepararExclusivamenteEnBaseDeVerificacion() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        run = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede", Integer.class, "PG " + run);
        area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area", Integer.class, "PG " + run, sede);
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria", Integer.class, "PG " + run);
        subcategoria = jdbc.queryForObject("INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria", Integer.class, "PG " + run, categoria);
        labA = lab("A");
        labB = lab("B");
        adminId = user("ADMIN", "admin");
        int gestorId = user("GESTOR", "gestor");
        int lectorId = user("LECTOR", "lector");
        user("LECTOR", "vacio");
        admin = token("admin"); gestor = token("gestor"); lector = token("lector"); vacio = token("vacio");
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?),(?,?)", gestorId, labA, lectorId, labA);
        equipment.add(equipo("0", "Oscilo" + run, labA, "OPERATIVO", false, "Marca" + run, "Modelo" + run));
        equipment.add(equipo("1", "Sensor 100%_!" + run, labA, "OPERATIVO", false, null, null));
        equipment.add(equipo("2", "Control" + run, labA, "OPERATIVO", true, null, null));
        equipment.add(equipo("3", "Motor" + run, labA, "MANTENIMIENTO", true, null, null));
        equipment.add(equipo("4", "Baja" + run, labA, "BAJA", false, null, null));
        equipment.add(equipo("5", "Oscilo" + run, labB, "OPERATIVO", false, null, null));
        equipment.add(equipo("6", "Fuera" + run, labB, "OPERATIVO", false, null, null));
        jdbc.update("UPDATE equipo SET id_responsable=? WHERE id_equipo=?", gestorId, equipment.getFirst());
        audit.add(evento(equipment.get(0), "CREAR", "2026-10-01 12:00:00+00"));
        audit.add(evento(equipment.get(1), "CREAR", "2026-10-01 12:00:00+00"));
        audit.add(evento(equipment.get(2), "CREAR", "2026-10-01 12:00:00+00"));
        audit.add(evento(equipment.get(0), "EDITAR", "2026-09-30 23:59:59+00"));
        audit.add(evento(equipment.get(0), "CREAR", "2026-10-02 00:00:00+00"));
    }

    @AfterEach
    void limpiarSoloFixturesPropios() {
        if (client != null) client.close();
        for (int id : users) jdbc.update("DELETE FROM auditoria WHERE id_usuario_actor=?", id);
        jdbc.update("DELETE FROM equipo WHERE id_laboratorio IN (?,?)", labA, labB);
        for (int id : users) jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario=?", id);
        for (int id : users) jdbc.update("DELETE FROM usuario WHERE id_usuario=?", id);
        jdbc.update("DELETE FROM laboratorio WHERE id_area=?", area);
        jdbc.update("DELETE FROM area WHERE id_sede=?", sede);
        jdbc.update("DELETE FROM sede WHERE id_sede=?", sede);
        jdbc.update("DELETE FROM subcategoria WHERE id_categoria=?", categoria);
        jdbc.update("DELETE FROM categoria WHERE id_categoria=?", categoria);
        users.clear(); equipment.clear(); audit.clear();
    }

    @Test
    void arrayAnteriorYAliasAdminSeConservanYLaPaginaTieneOrdenMetadatosYDefaults() throws Exception {
        String base = "/api/equipos?idLaboratorio=" + labA;
        JsonNode legacy = get(base, admin);
        assertTrue(legacy.isArray()); assertEquals(5, legacy.size());
        assertIds(legacy, equipment.subList(0, 5));
        JsonNode first = get(base + "&page=0&size=2", admin);
        assertPagina(first, 0, 2, 5, 3, true, false);
        assertIds(first.path("content"), equipment.subList(0, 2));
        assertEquals(first, get("/api/admin/equipos?idLaboratorio=" + labA + "&page=0&size=2", admin));
        JsonNode last = get(base + "&page=2&size=2", admin);
        assertPagina(last, 2, 2, 5, 3, false, true);
        assertIds(last.path("content"), List.of(equipment.get(4)));
        JsonNode outside = get(base + "&page=9&size=2", admin);
        assertPagina(outside, 9, 2, 5, 3, false, true);
        assertTrue(outside.path("content").isEmpty());
        assertPagina(get(base + "&page=1", admin), 1, 20, 5, 1, false, true);
        assertPagina(get(base + "&size=3", admin), 0, 3, 5, 2, true, false);
        assertPagina(get(base + "&page=2147483647&size=1", admin), Integer.MAX_VALUE, 1, 5, 5, false, true);
    }

    @Test
    void conteoContenidoYBusquedaRespetanElAlcanceAntesDePaginar() throws Exception {
        for (String actor : List.of(gestor, lector)) {
            JsonNode page = get("/api/equipos?page=0&size=2&q=" + run, actor);
            assertPagina(page, 0, 2, 5, 3, true, false);
            assertIds(page.path("content"), equipment.subList(0, 2));
            assertPagina(get("/api/equipos?page=0&q=" + encode("Fuera" + run), actor), 0, 20, 0, 0, true, true);
            error(403, enviar("/api/equipos?page=0&idLaboratorio=" + labB, actor));
        }
        assertPagina(get("/api/equipos?page=0&q=" + run, vacio), 0, 20, 0, 0, true, true);
        jdbc.update("UPDATE laboratorio SET activo=false WHERE id_laboratorio=?", labB);
        assertPagina(get("/api/admin/equipos?page=0&size=2&idLaboratorio=" + labB, admin), 0, 2, 2, 1, true, true);
        error(403, enviar("/api/admin/equipos?page=0", gestor));
        error(403, enviar("/api/admin/auditoria?page=0", lector));
    }

    @Test
    void qBuscaEnServidorAntesDelLimiteYConservaLaConcatenacionDeCamposDeLaInterfaz() throws Exception {
        String search = "  oScIlO" + run + "  ";
        JsonNode first = get("/api/equipos?page=0&size=1&q=" + encode(search), admin);
        assertPagina(first, 0, 1, 2, 2, true, false);
        assertIds(first.path("content"), List.of(equipment.get(0)));
        JsonNode second = get("/api/equipos?page=1&size=1&q=" + encode(search), admin);
        assertPagina(second, 1, 1, 2, 2, false, true);
        assertIds(second.path("content"), List.of(equipment.get(5)));
        assertIds(get("/api/equipos?q=" + encode(search), admin), List.of(equipment.get(0), equipment.get(5)));
        for (String q : List.of("Marca" + run, "Modelo" + run, codigo("0"),
                "Oscilo" + run + " " + codigo("0"), "Modelo" + run + " Laboratorio A " + run)) {
            assertIds(get("/api/equipos?page=0&q=" + encode(q), admin).path("content"), List.of(equipment.get(0)));
        }
        assertPagina(get("/api/equipos?page=0&q=" + encode("Laboratorio A " + run), admin), 0, 20, 5, 1, true, true);
        assertPagina(get("/api/equipos?page=0&idLaboratorio=" + labA + "&q=" + encode("   "), admin), 0, 20, 5, 1, true, true);
    }

    @Test
    void comodinesYTextoSqlEnQSeTratanComoTextoLiteral() throws Exception {
        for (String q : List.of("%", "_", "!", "100%_!")) {
            JsonNode page = get("/api/equipos?page=0&q=" + encode(q), admin);
            assertPagina(page, 0, 20, 1, 1, true, true);
            assertIds(page.path("content"), List.of(equipment.get(1)));
        }
        assertPagina(get("/api/equipos?page=0&q=" + encode("' or 1=1 --"), admin), 0, 20, 0, 0, true, true);
    }

    @Test
    void filtrosDeEquipoSeCombinanConQYConElConteo() throws Exception {
        JsonNode page = get("/api/equipos?page=0&size=1&idLaboratorio=" + labA
                + "&idSubcategoria=" + subcategoria + "&estado=MANTENIMIENTO&requiereMantenimiento=true&q="
                + encode("Motor" + run), admin);
        assertPagina(page, 0, 1, 1, 1, true, true);
        assertIds(page.path("content"), List.of(equipment.get(3)));
        assertPagina(get("/api/equipos?page=0&idLaboratorio=" + labA + "&estado=BAJA", admin), 0, 20, 1, 1, true, true);
    }

    @Test
    void auditoriaPaginaAntesDeMapearFiltraConDiasUtcYDesempataPorIdDescendente() throws Exception {
        String base = "/api/admin/auditoria?idUsuario=" + adminId;
        List<Integer> expected = List.of(audit.get(4), audit.get(2), audit.get(1), audit.get(0), audit.get(3));
        JsonNode legacy = get(base, admin);
        assertTrue(legacy.isArray()); assertIds(legacy, expected);
        JsonNode first = get(base + "&page=0&size=2", admin);
        assertPagina(first, 0, 2, 5, 3, true, false);
        assertIds(first.path("content"), expected.subList(0, 2));
        assertIds(get(base + "&page=1&size=2", admin).path("content"), expected.subList(2, 4));
        assertPagina(get(base + "&page=99&size=2", admin), 99, 2, 5, 3, false, true);
        assertPagina(get(base + "&size=1", admin), 0, 1, 5, 5, true, false);
        String date = base + "&entidad=equipo&accion=CREAR&fechaDesde=2026-10-01&fechaHasta=2026-10-01";
        JsonNode filtered = get(date + "&page=0&size=2", admin);
        assertPagina(filtered, 0, 2, 3, 2, true, false);
        assertIds(filtered.path("content"), List.of(audit.get(2), audit.get(1)));
        assertPagina(get(date + "&idEntidad=" + equipment.get(0) + "&page=0", admin), 0, 20, 1, 1, true, true);
        error(400, enviar(base + "&page=0&fechaDesde=2026-10-02&fechaHasta=2026-10-01", admin));
        error(403, enviar(base + "&page=0", gestor));
    }

    @Test
    void paginacionInvalidaProduceApiError400YNoDebilitaLaAutenticacion() throws Exception {
        for (String path : List.of("/api/equipos", "/api/admin/equipos", "/api/admin/auditoria")) {
            for (String invalid : List.of("page=-1", "size=0", "size=101", "page=abc", "size=abc",
                    "page=2147483648", "size=2147483648", "page=2147483647&size=100", "page=", "size=")) {
                error(400, enviar(path + "?" + invalid, admin));
            }
            error(401, enviar(path + "?page=0&size=20", null));
        }
        for (String path : List.of("/api/equipos", "/api/admin/equipos")) {
            error(400, enviar(path + "?page=0&q=" + "x".repeat(201), admin));
            assertPagina(get(path + "?page=0&q=" + "x".repeat(200), admin), 0, 20, 0, 0, true, true);
        }
    }

    @Test
    void consultasPaginadasNoEscribenAuditoriaNiCambianInventario() throws Exception {
        int beforeEquipment = contar("equipo", "id_laboratorio IN (?,?)", labA, labB);
        int beforeAudit = contar("auditoria", "id_usuario_actor=?", adminId);
        get("/api/equipos?page=0&size=2&q=" + run, admin);
        get("/api/admin/equipos?page=0&size=2&q=" + run, admin);
        get("/api/admin/auditoria?page=0&size=2&idUsuario=" + adminId, admin);
        assertEquals(beforeEquipment, contar("equipo", "id_laboratorio IN (?,?)", labA, labB));
        assertEquals(beforeAudit, contar("auditoria", "id_usuario_actor=?", adminId));
    }

    private int lab(String label) {
        return jdbc.queryForObject("INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio",
                Integer.class, "Laboratorio " + label + " " + run, "PG-" + label + "-" + run, area);
    }
    private String codigo(String suffix) { return "PG-" + suffix + "-" + run; }
    private int equipo(String suffix, String nombre, int lab, String estado, boolean mantenimiento, String marca, String modelo) {
        return jdbc.queryForObject("INSERT INTO equipo(codigo_interno,nombre,id_laboratorio,id_subcategoria,estado,requiere_mantenimiento,marca,modelo) "
                + "VALUES (?,?,?,?,?,?,?,?) RETURNING id_equipo", Integer.class,
                codigo(suffix), nombre, lab, subcategoria, estado, mantenimiento, marca, modelo);
    }
    private int user(String rol, String label) {
        int id = jdbc.queryForObject("INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol) "
                + "SELECT ?,?,?,?, ?,id_rol FROM rol WHERE nombre=? RETURNING id_usuario", Integer.class,
                "pg_" + label + "_" + run, "Prueba", "Paginación", "pg_" + label + "_" + run + "@example.invalid",
                encoder.encode(UUID.randomUUID().toString()), rol);
        users.add(id);
        return id;
    }
    private String token(String label) {
        return jwtService.generateToken(usuarios.loadUserByUsername("pg_" + label + "_" + run));
    }
    private int evento(int idEquipo, String accion, String fecha) {
        return jdbc.queryForObject("INSERT INTO auditoria(id_usuario_actor,username_actor,accion,entidad,id_entidad,cambios,fecha) "
                + "VALUES (?, ?, ?, 'equipo', ?, 'Fixture de paginación', ?::timestamptz) RETURNING id_auditoria",
                Integer.class, adminId, "pg_admin_" + run, accion, idEquipo, fecha);
    }
    private int contar(String tabla, String where, Object... args) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE " + where, Integer.class, args);
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private HttpResponse<String> enviar(String path, String bearer) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode get(String path, String bearer) throws Exception {
        HttpResponse<String> response = enviar(path, bearer);
        assertEquals(200, response.statusCode(), response.body());
        seguro(response.body());
        return mapper.readTree(response.body());
    }
    private void error(int expected, HttpResponse<String> response) {
        assertEquals(expected, response.statusCode(), response.body());
        assertEquals(expected, mapper.readTree(response.body()).path("status").asInt());
        seguro(response.body());
    }
    private void seguro(String body) {
        for (String secret : List.of("passwordHash", "password_hash", "$2a$", "$2b$", "JWT_SECRET")) {
            assertFalse(body.contains(secret));
        }
    }
    private void assertPagina(JsonNode data, int number, int size, long total, int pages, boolean first, boolean last) {
        assertTrue(data.isObject()); assertEquals(7, data.size());
        assertTrue(data.path("content").isArray());
        assertEquals(number, data.path("number").asInt()); assertEquals(size, data.path("size").asInt());
        assertEquals(total, data.path("totalElements").asLong()); assertEquals(pages, data.path("totalPages").asInt());
        assertEquals(first, data.path("first").asBoolean()); assertEquals(last, data.path("last").asBoolean());
    }
    private void assertIds(JsonNode content, List<Integer> ids) {
        assertTrue(content.isArray());
        List<Integer> actual = new ArrayList<>();
        content.forEach(item -> actual.add(item.path("id").asInt()));
        assertEquals(ids, actual);
    }
}
