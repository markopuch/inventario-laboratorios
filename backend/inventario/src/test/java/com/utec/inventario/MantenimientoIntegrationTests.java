package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(90)
class MantenimientoIntegrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @LocalServerPort private int port;
    private HttpClient client;
    private String run;
    private int sede;
    private int area;
    private int categoria;
    private int subcategoria;
    private int labA;
    private int labB;
    private int admin;
    private int gestor;
    private int lector;
    private String jwtAdmin;
    private String jwtGestor;
    private String jwtLector;
    private final List<Integer> usuariosPropios = new ArrayList<>();

    @BeforeEach
    void prepararFixturesEnBaseTemporal() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        run = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede", Integer.class, "Mant " + run);
        area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area", Integer.class, "Mant " + run, sede);
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria", Integer.class, "Mant " + run);
        subcategoria = jdbc.queryForObject("INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria", Integer.class, "Mant " + run, categoria);
        labA = nuevoLab("A"); labB = nuevoLab("B");
        admin = nuevoUsuario("ADMIN"); gestor = nuevoUsuario("GESTOR"); lector = nuevoUsuario("LECTOR");
        jwtAdmin = token("ADMIN"); jwtGestor = token("GESTOR"); jwtLector = token("LECTOR");
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?),(?,?)", gestor, labA, lector, labA);
    }

    @AfterEach
    void limpiarUnicamenteFixturesPropios() {
        if (client != null) client.close();
        for (int usuario : usuariosPropios) {
            jdbc.update("DELETE FROM auditoria WHERE id_usuario_actor=?", usuario);
        }
        jdbc.update("DELETE FROM mantenimiento WHERE id_equipo IN (SELECT id_equipo FROM equipo WHERE id_subcategoria=?)", subcategoria);
        jdbc.update("DELETE FROM movimiento_equipo WHERE id_equipo IN (SELECT id_equipo FROM equipo WHERE id_subcategoria=?)", subcategoria);
        jdbc.update("DELETE FROM equipo WHERE id_subcategoria=?", subcategoria);
        for (int usuario : usuariosPropios) {
            jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario=?", usuario);
            jdbc.update("DELETE FROM usuario WHERE id_usuario=?", usuario);
        }
        jdbc.update("DELETE FROM laboratorio WHERE id_area=?", area);
        jdbc.update("DELETE FROM area WHERE id_area=?", area);
        jdbc.update("DELETE FROM sede WHERE id_sede=?", sede);
        jdbc.update("DELETE FROM subcategoria WHERE id_categoria=?", categoria);
        jdbc.update("DELETE FROM categoria WHERE id_categoria=?", categoria);
    }

    @Test
    void flujoProgramarEditarIniciarCompletarTieneFechasDelServidorYConservaHistoria() throws Exception {
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        Map<String, Object> body = crearRequest(equipo, "2026-10-10", "PREVENTIVO");
        body.put("idResponsable", lector);
        var createdHttp = enviar("POST", "/api/mantenimientos", jwtAdmin, json(body));
        JsonNode creado = ok(201, createdHttp);
        int id = creado.path("id").asInt();
        assertTrue(createdHttp.headers().firstValue("Location").orElseThrow().endsWith("/api/mantenimientos/" + id));
        assertEquals("PROGRAMADO", creado.path("estado").asString());
        assertEquals("OPERATIVO", estadoEquipo(equipo));
        assertTrue(creado.path("fechaInicio").isNull()); assertTrue(creado.path("fechaFin").isNull());
        assertEquals(lector, creado.path("responsable").path("id").asInt());
        assertEquals(labA, creado.path("equipo").path("laboratorio").path("id").asInt());
        assertFalse(creado.has("estadoEquipoAnterior"));
        assertFalse(creado.path("fechaCreacion").asString().isBlank());
        Map<String, Object> edit = editarRequest("2026-10-11", "CALIBRACION");
        JsonNode editado = ok(200, enviar("PUT", ruta(id), jwtAdmin, json(edit)));
        assertEquals("2026-10-11", editado.path("fechaProgramada").asString());
        assertEquals("CALIBRACION", editado.path("tipo").asString());
        assertTrue(editado.path("responsable").isNull());
        JsonNode iniciado = cambiar(id, "EN_PROCESO", jwtGestor);
        assertEquals("MANTENIMIENTO", estadoEquipo(equipo));
        assertFalse(iniciado.path("fechaInicio").asString().isBlank());
        assertTrue(iniciado.path("fechaFin").isNull());
        JsonNode finalizado = cambiar(id, "COMPLETADO", jwtGestor);
        assertEquals("OPERATIVO", estadoEquipo(equipo));
        assertFalse(OffsetDateTime.parse(finalizado.path("fechaFin").asString()).isBefore(OffsetDateTime.parse(iniciado.path("fechaInicio").asString())));
        assertEquals("COMPLETADO", get(ruta(id), jwtLector).path("estado").asString());
        error(409, enviar("PUT", ruta(id), jwtAdmin, json(edit)));
        error(409, enviar("PATCH", ruta(id) + "/estado", jwtAdmin, json(Map.of("estado", "EN_PROCESO"))));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM mantenimiento WHERE id_equipo=?", Integer.class, equipo));
    }

    @Test
    void cancelarEnProcesoRestauraInoperativoYCancelarProgramadoNoCambiaEquipo() throws Exception {
        int equipo = nuevoEquipo(labA, "INOPERATIVO");
        int id = crear(equipo, "2026-10-10", "CORRECTIVO", jwtAdmin);
        cambiar(id, "EN_PROCESO", jwtAdmin);
        assertEquals("MANTENIMIENTO", estadoEquipo(equipo));
        assertEquals("INOPERATIVO", jdbc.queryForObject("SELECT estado_equipo_anterior FROM mantenimiento WHERE id_mantenimiento=?", String.class, id));
        cambiar(id, "CANCELADO", jwtAdmin);
        assertEquals("INOPERATIVO", estadoEquipo(equipo));
        int segundo = crear(equipo, "2026-10-12", "OTRO", jwtAdmin);
        JsonNode cancelado = cambiar(segundo, "CANCELADO", jwtAdmin);
        assertTrue(cancelado.path("fechaInicio").isNull());
        assertFalse(cancelado.path("fechaFin").asString().isBlank());
        assertEquals("INOPERATIVO", estadoEquipo(equipo));
    }

    @Test
    void permisosJwtLecturaEscrituraYAlcanceSeValidanEnBackend() throws Exception {
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        int otro = nuevoEquipo(labB, "OPERATIVO");
        String request = json(crearRequest(equipo, "2026-10-10", "PREVENTIVO"));
        for (String jwt : new String[] { null, "invalido" }) {
            error(401, enviar("GET", "/api/mantenimientos", jwt, null));
            error(401, enviar("POST", "/api/mantenimientos", jwt, request));
        }
        error(403, enviar("POST", "/api/mantenimientos", jwtLector, request));
        error(403, enviar("POST", "/api/mantenimientos", jwtGestor, json(crearRequest(otro, "2026-10-10", "PREVENTIVO"))));
        int propio = crear(equipo, "2026-10-10", "PREVENTIVO", jwtGestor);
        int ajeno = crear(otro, "2026-10-10", "PREVENTIVO", jwtAdmin);
        assertIds(get("/api/mantenimientos", jwtLector), propio);
        assertIds(get("/api/mantenimientos", jwtGestor), propio);
        assertTrue(ids(get("/api/mantenimientos", jwtAdmin)).containsAll(List.of(propio, ajeno)));
        error(403, enviar("GET", ruta(ajeno), jwtLector, null));
        error(403, enviar("GET", "/api/mantenimientos?idEquipo=" + otro, jwtGestor, null));
        error(403, enviar("GET", "/api/mantenimientos?idLaboratorio=" + labB, jwtLector, null));
        error(403, enviar("PUT", ruta(propio), jwtLector, json(editarRequest("2026-10-11", "OTRO"))));
        error(403, enviar("PATCH", ruta(propio) + "/estado", jwtLector, json(Map.of("estado", "CANCELADO"))));
        jdbc.update("UPDATE usuario_laboratorio SET activo=false WHERE id_usuario=?", gestor);
        assertTrue(get("/api/mantenimientos", jwtGestor).isEmpty());
        error(403, enviar("GET", ruta(propio), jwtGestor, null));
    }

    @Test
    void filtrosFechaProgramadaSonInclusivosYSePuedenCombinar() throws Exception {
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        int a = crear(equipo, "2026-10-10", "PREVENTIVO", jwtAdmin);
        int b = crear(equipo, "2026-10-11", "PREVENTIVO", jwtAdmin);
        int c = crear(equipo, "2026-10-12", "CORRECTIVO", jwtAdmin);
        String base = "/api/mantenimientos?idEquipo=" + equipo;
        assertIds(get(base + "&fechaDesde=2026-10-10&fechaHasta=2026-10-12", jwtLector), a, b, c);
        assertIds(get(base + "&fechaDesde=2026-10-11&fechaHasta=2026-10-11", jwtLector), b);
        assertIds(get(base + "&tipo=PREVENTIVO&estado=PROGRAMADO&idLaboratorio=" + labA, jwtGestor), a, b);
        cambiar(c, "CANCELADO", jwtAdmin);
        assertIds(get(base + "&estado=CANCELADO", jwtLector), c);
        for (String filter : List.of("&fechaDesde=2026-10-12&fechaHasta=2026-10-10", "&fechaDesde=no-fecha", "&tipo=INVALIDO", "&estado=INVALIDO", "&idLaboratorio=0")) {
            error(400, enviar("GET", base + filter, jwtAdmin, null));
        }
    }

    @Test
    void validaCamposEstrictoSinAceptarActorEquipoCambiosDeFechasOEstadosDelCliente() throws Exception {
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        for (String campo : List.of("estado", "fechaInicio", "fechaFin", "idUsuarioActor", "rol", "estadoEquipoAnterior")) {
            var body = crearRequest(equipo, "2026-10-10", "PREVENTIVO"); body.put(campo, "cliente");
            error(400, enviar("POST", "/api/mantenimientos", jwtAdmin, json(body)));
        }
        for (String invalid : List.of("{}", "null", "{", "{\"idEquipo\":0}", "{\"idEquipo\":" + equipo + ",\"tipo\":0,\"descripcion\":\"Revisión\",\"fechaProgramada\":\"2026-10-10\"}")) {
            error(400, enviar("POST", "/api/mantenimientos", jwtAdmin, invalid));
        }
        var body = crearRequest(equipo, "2026-10-10", "PREVENTIVO"); body.put("descripcion", "   ");
        error(400, enviar("POST", "/api/mantenimientos", jwtAdmin, json(body)));
        body.put("descripcion", "x".repeat(2001));
        error(400, enviar("POST", "/api/mantenimientos", jwtAdmin, json(body)));
        int id = crear(equipo, "2026-10-10", "PREVENTIVO", jwtAdmin);
        var edit = editarRequest("2026-10-12", "OTRO"); edit.put("idEquipo", equipo);
        error(400, enviar("PUT", ruta(id), jwtAdmin, json(edit)));
        error(400, enviar("PATCH", ruta(id) + "/estado", jwtAdmin, json(Map.of("estado", "EN_PROCESO", "fechaInicio", "2026-10-10T00:00:00Z"))));
        error(400, enviar("PATCH", ruta(id) + "/estado", jwtAdmin, "{}"));
        assertEquals("PROGRAMADO", get(ruta(id), jwtAdmin).path("estado").asString());
    }

    @Test
    void bajaReferenciasAusentesResponsableInactivoYTransicionesInvalidasDevuelven409O404() throws Exception {
        int baja = nuevoEquipo(labA, "BAJA");
        error(409, enviar("POST", "/api/mantenimientos", jwtAdmin, json(crearRequest(baja, "2026-10-10", "PREVENTIVO"))));
        error(404, enviar("POST", "/api/mantenimientos", jwtAdmin, json(crearRequest(Integer.MAX_VALUE, "2026-10-10", "PREVENTIVO"))));
        error(404, enviar("GET", ruta(Integer.MAX_VALUE), jwtAdmin, null));
        error(404, enviar("GET", "/api/mantenimientos?idLaboratorio=" + Integer.MAX_VALUE, jwtAdmin, null));
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        var body = crearRequest(equipo, "2026-10-10", "PREVENTIVO"); body.put("idResponsable", Integer.MAX_VALUE);
        error(404, enviar("POST", "/api/mantenimientos", jwtAdmin, json(body)));
        jdbc.update("UPDATE usuario SET activo=false WHERE id_usuario=?", lector);
        body.put("idResponsable", lector);
        error(409, enviar("POST", "/api/mantenimientos", jwtAdmin, json(body)));
        int id = crear(equipo, "2026-10-10", "PREVENTIVO", jwtAdmin);
        error(409, enviar("PATCH", ruta(id) + "/estado", jwtAdmin, json(Map.of("estado", "COMPLETADO"))));
        ok(204, enviar("DELETE", "/api/equipos/" + equipo, jwtAdmin, null));
        error(409, enviar("PATCH", ruta(id) + "/estado", jwtAdmin, json(Map.of("estado", "EN_PROCESO"))));
        cambiar(id, "CANCELADO", jwtAdmin);
        assertEquals("BAJA", estadoEquipo(equipo));
    }

    @Test
    void equipoConMantenimientoEnProcesoNoPuedeEditarseDarseDeBajaNiTrasladarse() throws Exception {
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        int id = crear(equipo, "2026-10-10", "PREVENTIVO", jwtAdmin);
        cambiar(id, "EN_PROCESO", jwtAdmin);
        var edit = Map.of("nombre", "Cambio", "estado", "OPERATIVO", "requiereMantenimiento", true, "idSubcategoria", subcategoria);
        error(409, enviar("PUT", "/api/equipos/" + equipo, jwtAdmin, json(edit)));
        error(409, enviar("DELETE", "/api/equipos/" + equipo, jwtAdmin, null));
        error(409, enviar("POST", "/api/equipos/" + equipo + "/traslados", jwtAdmin, json(Map.of("idLaboratorioDestino", labB, "motivo", "No permitido durante mantenimiento"))));
        assertEquals("MANTENIMIENTO", estadoEquipo(equipo));
        assertEquals(labA, jdbc.queryForObject("SELECT id_laboratorio FROM equipo WHERE id_equipo=?", Integer.class, equipo));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM movimiento_equipo WHERE id_equipo=?", Integer.class, equipo));
    }

    @Test
    void concurrenciaDeIniciosPermiteUnSoloMantenimientoEnProceso() throws Exception {
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        int a = crear(equipo, "2026-10-10", "PREVENTIVO", jwtAdmin);
        int b = crear(equipo, "2026-10-11", "CORRECTIVO", jwtAdmin);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch salir = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var uno = executor.submit(() -> { listos.countDown(); assertTrue(salir.await(10, TimeUnit.SECONDS)); return enviar("PATCH", ruta(a) + "/estado", jwtAdmin, json(Map.of("estado", "EN_PROCESO"))); });
            var dos = executor.submit(() -> { listos.countDown(); assertTrue(salir.await(10, TimeUnit.SECONDS)); return enviar("PATCH", ruta(b) + "/estado", jwtGestor, json(Map.of("estado", "EN_PROCESO"))); });
            assertTrue(listos.await(10, TimeUnit.SECONDS)); salir.countDown();
            HttpResponse<String> r1 = uno.get(30, TimeUnit.SECONDS);
            HttpResponse<String> r2 = dos.get(30, TimeUnit.SECONDS);
            assertEquals(new TreeSet<>(List.of(200, 409)), new TreeSet<>(List.of(r1.statusCode(), r2.statusCode())));
            ok(r1.statusCode(), r1); ok(r2.statusCode(), r2);
        } finally { salir.countDown(); }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM mantenimiento WHERE id_equipo=? AND estado='EN_PROCESO'", Integer.class, equipo));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM mantenimiento WHERE id_equipo=? AND estado='PROGRAMADO'", Integer.class, equipo));
        assertEquals("MANTENIMIENTO", estadoEquipo(equipo));
    }

    @Test
    void restriccionesSqlEvitanTiposCiclosYActivosDuplicados() throws Exception {
        int equipo = nuevoEquipo(labA, "OPERATIVO");
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO mantenimiento(id_equipo,tipo,descripcion,fecha_programada) VALUES (?,'INVALIDO','Revisión',DATE '2026-10-10')", equipo));
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO mantenimiento(id_equipo,tipo,descripcion,fecha_programada,estado) VALUES (?,'OTRO','Revisión',DATE '2026-10-10','EN_PROCESO')", equipo));
        int id = crear(equipo, "2026-10-10", "PREVENTIVO", jwtAdmin);
        cambiar(id, "EN_PROCESO", jwtAdmin);
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO mantenimiento(id_equipo,tipo,descripcion,fecha_programada,estado,fecha_inicio,estado_equipo_anterior) VALUES (?,'OTRO','Revisión',DATE '2026-10-10','EN_PROCESO',CURRENT_TIMESTAMP,'OPERATIVO')", equipo));
    }

    private int nuevoLab(String label) { return jdbc.queryForObject("INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio", Integer.class, "Mant " + label, "MANT-" + label + "-" + run, area); }
    private int nuevoUsuario(String rol) {
        int id = jdbc.queryForObject("INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol) SELECT ?,?,?,?,seed.password_hash,r.id_rol FROM usuario seed JOIN rol r ON r.nombre=? WHERE seed.username='marko' RETURNING id_usuario", Integer.class, username(rol), "Prueba", "Mantenimiento", username(rol) + "@example.invalid", rol);
        usuariosPropios.add(id); return id;
    }
    private int nuevoEquipo(int laboratorio, String estado) { return jdbc.queryForObject("INSERT INTO equipo(codigo_interno,nombre,estado,id_subcategoria,id_laboratorio) VALUES (?,?,?,?,?) RETURNING id_equipo", Integer.class, "MANT-" + UUID.randomUUID(), "Equipo mantenimiento", estado, subcategoria, laboratorio); }
    private String username(String rol) { return "mant_" + rol.toLowerCase(java.util.Locale.ROOT) + "_" + run; }
    private String token(String rol) { return jwtService.generateToken(usuarioService.loadUserByUsername(username(rol))); }
    private String ruta(int id) { return "/api/mantenimientos/" + id; }
    private String estadoEquipo(int id) { return jdbc.queryForObject("SELECT estado FROM equipo WHERE id_equipo=?", String.class, id); }
    private Map<String, Object> crearRequest(int equipo, String fecha, String tipo) { var body = editarRequest(fecha, tipo); body.put("idEquipo", equipo); return body; }
    private Map<String, Object> editarRequest(String fecha, String tipo) { return new LinkedHashMap<>(Map.of("tipo", tipo, "descripcion", "  Revisión del equipo  ", "fechaProgramada", fecha, "observaciones", "  Programación  ")); }
    private int crear(int equipo, String fecha, String tipo, String jwt) throws Exception { return ok(201, enviar("POST", "/api/mantenimientos", jwt, json(crearRequest(equipo, fecha, tipo)))).path("id").asInt(); }
    private JsonNode cambiar(int id, String estado, String jwt) throws Exception { return ok(200, enviar("PATCH", ruta(id) + "/estado", jwt, json(Map.of("estado", estado)))); }
    private String json(Map<String, ?> body) { return mapper.writeValueAsString(body); }
    private JsonNode get(String path, String jwt) throws Exception { return ok(200, enviar("GET", path, jwt, null)); }
    private HttpResponse<String> enviar(String method, String path, String jwt, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode ok(int status, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        for (String secreto : List.of("passwordHash", "password_hash", "$2a$", "$2b$", "JWT_SECRET", "DB_PASSWORD")) assertFalse(response.body().contains(secreto));
        return response.body().isBlank() ? mapper.readTree("null") : mapper.readTree(response.body());
    }
    private void error(int status, HttpResponse<String> response) {
        var error = ok(status, response); assertEquals(status, error.path("status").asInt());
        assertFalse(error.path("message").asString().isBlank()); assertFalse(error.has("trace"));
        assertFalse(response.body().contains("org.hibernate")); assertFalse(response.body().contains("fk_mantenimiento"));
    }
    private TreeSet<Integer> ids(JsonNode rows) { assertTrue(rows.isArray()); var result = new TreeSet<Integer>(); rows.forEach(row -> result.add(row.path("id").asInt())); return result; }
    private void assertIds(JsonNode rows, Integer... esperados) { assertEquals(new TreeSet<>(List.of(esperados)), ids(rows)); }
}
