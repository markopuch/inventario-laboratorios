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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
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
class MovimientoEquipoIntegrationTests {
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
    private int labC;
    private int admin;
    private int gestor;
    private int lector;
    private String jwtAdmin;
    private String jwtGestor;
    private String jwtLector;
    private final List<Integer> usuariosPropios = new ArrayList<>();

    @BeforeEach
    void prepararDatosPropios() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        run = UUID.randomUUID().toString().replace("-", "").substring(0,16);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede", Integer.class, "S6 " + run);
        area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area", Integer.class, "S6 " + run, sede);
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria", Integer.class, "S6 " + run);
        subcategoria = jdbc.queryForObject("INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria", Integer.class, "S6 " + run, categoria);
        labA = nuevoLab("A"); labB = nuevoLab("B"); labC = nuevoLab("C");
        admin = nuevoUsuario("ADMIN"); gestor = nuevoUsuario("GESTOR"); lector = nuevoUsuario("LECTOR");
        jwtAdmin = token("ADMIN"); jwtGestor = token("GESTOR"); jwtLector = token("LECTOR");
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?),(?,?),(?,?)", gestor, labA, gestor, labB, lector, labA);
    }

    @AfterEach
    void limpiarSoloFixturesPropios() {
        if (client != null) client.close();
        jdbc.update("DELETE FROM movimiento_equipo WHERE id_equipo IN (SELECT id_equipo FROM equipo WHERE id_subcategoria=?)", subcategoria);
        jdbc.update("DELETE FROM equipo WHERE id_subcategoria=?", subcategoria);
        for (int user : usuariosPropios) {
            jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario=?", user);
            jdbc.update("DELETE FROM usuario WHERE id_usuario=?", user);
        }
        jdbc.update("DELETE FROM laboratorio WHERE id_area=?", area);
        jdbc.update("DELETE FROM area WHERE id_area=?", area);
        jdbc.update("DELETE FROM sede WHERE id_sede=?", sede);
        jdbc.update("DELETE FROM subcategoria WHERE id_categoria=?", categoria);
        jdbc.update("DELETE FROM categoria WHERE id_categoria=?", categoria);
    }

    @Test
    void trasladoAdminActualizaEquipoYCreaEventoConOrigenActorYFechaDelServidor() throws Exception {
        int equipo = nuevoEquipo("ADMIN", "OPERATIVO");
        JsonNode before = get("/api/equipos/" + equipo, jwtAdmin);
        JsonNode response = trasladar(equipo, labB, jwtAdmin, "  Motivo válido  ", "  Armario B  ");
        JsonNode updated = response.path("equipo");
        JsonNode movement = response.path("movimiento");
        for (String field : List.of("id", "codigoInterno", "subcategoria", "responsable", "fechaCreacion", "estado")) {
            assertEquals(before.path(field), updated.path(field), field);
        }
        assertEquals(labB, updated.path("laboratorio").path("id").asInt());
        assertEquals("Armario B", updated.path("ubicacionInterna").asString());
        assertTrue(OffsetDateTime.parse(updated.path("fechaActualizacion").asString()).toInstant()
                .isAfter(OffsetDateTime.parse(before.path("fechaActualizacion").asString()).toInstant()));
        assertEquals(equipo, movement.path("equipo").path("id").asInt());
        assertEquals(3, movement.path("equipo").size());
        assertEquals(labA, movement.path("laboratorioOrigen").path("id").asInt());
        assertEquals(labB, movement.path("laboratorioDestino").path("id").asInt());
        assertEquals(3, movement.path("laboratorioOrigen").size());
        assertEquals(3, movement.path("laboratorioDestino").size());
        assertEquals(admin, movement.path("actor").path("id").asInt());
        assertEquals(4, movement.path("actor").size());
        assertEquals("TRASLADO", movement.path("tipoMovimiento").asString());
        assertEquals("Motivo válido", movement.path("motivo").asString());
        assertFalse(movement.path("fechaMovimiento").asString().isBlank());
        assertEquals(1, contarMovimientos(equipo));
        Map<String,Object> row = jdbc.queryForMap("SELECT id_laboratorio_origen,id_laboratorio_destino,id_usuario_actor,tipo_movimiento,motivo FROM movimiento_equipo WHERE id_equipo=?", equipo);
        assertEquals(labA, row.get("id_laboratorio_origen"));
        assertEquals(labB, row.get("id_laboratorio_destino"));
        assertEquals(admin, row.get("id_usuario_actor"));
        assertEquals("TRASLADO", row.get("tipo_movimiento"));
        assertEquals("Motivo válido", row.get("motivo"));
        JsonNode persisted = get("/api/equipos/" + equipo, jwtAdmin);
        for (String field : List.of("id", "codigoInterno", "serieUtec", "numeroSerie", "nombre", "marca", "modelo",
                "estado", "anio", "ordenCompra", "ubicacionInterna", "comentario", "requiereMantenimiento",
                "subcategoria", "laboratorio", "responsable", "fechaCreacion")) {
            assertEquals(updated.path(field), persisted.path(field), field);
        }
        // Java conserva nanosegundos; PostgreSQL almacena TIMESTAMPTZ con precisión de microsegundos.
        Duration timestampDifference = Duration.between(
                OffsetDateTime.parse(updated.path("fechaActualizacion").asString()).toInstant(),
                OffsetDateTime.parse(persisted.path("fechaActualizacion").asString()).toInstant()).abs();
        assertTrue(timestampDifference.compareTo(Duration.ofNanos(1000)) <= 0,
                "La fecha persistida debe coincidir dentro de la precisión de PostgreSQL");
    }

    @Test
    void todosLosEstadosNoBajaSeTrasladanYUbicacionOmitidaNulaOBlancaSeLimpia() throws Exception {
        Object[] locations = { null, "   ", "" };
        String[] states = { "OPERATIVO", "MANTENIMIENTO", "INOPERATIVO" };
        for (int i=0; i<states.length; i++) {
            int equipo = nuevoEquipo(states[i], states[i]);
            Map<String,Object> body = request(labB, "Motivo " + states[i]);
            if (i != 0) body.put("ubicacionInternaDestino", locations[i-1]);
            JsonNode response = ok(200, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(body)));
            assertEquals(states[i], response.path("equipo").path("estado").asString());
            assertTrue(response.path("equipo").path("ubicacionInterna").isNull());
            assertEquals(1, contarMovimientos(equipo));
        }
        int boundaries = nuevoEquipo("LIMITES", "OPERATIVO");
        JsonNode response = trasladar(boundaries, labB, jwtAdmin, "m".repeat(500), "u".repeat(200));
        assertEquals(500, response.path("movimiento").path("motivo").asString().length());
        assertEquals(200, response.path("equipo").path("ubicacionInterna").asString().length());
    }

    @Test
    void gestorNecesitaOrigenYDestinoYLecturaNoConcedePermisoDeTraslado() throws Exception {
        int equipo = nuevoEquipo("ALCANCE", "OPERATIVO");
        Map<String,Object> original = estadoEquipo(equipo);
        asignar(gestor, labA);
        error(403, enviar("POST", trasladoRuta(equipo), jwtGestor, json(request(labB, "Solo origen"))));
        assertEquals(original, estadoEquipo(equipo));
        asignar(gestor, labB);
        error(403, enviar("POST", trasladoRuta(equipo), jwtGestor, json(request(labB, "Solo destino"))));
        assertEquals(original, estadoEquipo(equipo));
        error(403, enviar("POST", trasladoRuta(equipo), jwtLector, json(request(labB, "Lector"))));
        assertEquals(0, contarMovimientos(equipo));
        asignar(gestor, labA, labB);
        JsonNode response = trasladar(equipo, labB, jwtGestor, "Permiso en ambos", null);
        assertEquals(gestor, response.path("movimiento").path("actor").path("id").asInt());
        assertEquals(1, contarMovimientos(equipo));
        asignar(gestor);
        Map<String,Object> moved = estadoEquipo(equipo);
        error(403, enviar("POST", trasladoRuta(equipo), jwtGestor, json(request(labA, "JWT anterior"))));
        assertEquals(moved, estadoEquipo(equipo));
        assertEquals(1, contarMovimientos(equipo));
    }

    @Test
    void referenciasMismoDestinoYBajaNoCambianEquipoNiInsertanMovimiento() throws Exception {
        int equipo = nuevoEquipo("INVALIDO", "OPERATIVO");
        Map<String,Object> original = estadoEquipo(equipo);
        error(404, enviar("POST", trasladoRuta(Integer.MAX_VALUE), jwtAdmin, json(request(labB, "Falta equipo"))));
        error(404, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(request(Integer.MAX_VALUE, "Falta destino"))));
        error(409, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(request(labA, "Mismo destino"))));
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labC, jwtAdmin, null).statusCode());
        error(409, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(request(labC, "Destino inactivo"))));
        assertEquals(original, estadoEquipo(equipo));
        assertEquals(0, contarMovimientos(equipo));
        assertEquals(204, enviar("DELETE", "/api/equipos/" + equipo, jwtAdmin, null).statusCode());
        Map<String,Object> baja = estadoEquipo(equipo);
        error(409, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(request(labB, "Equipo BAJA"))));
        assertEquals(baja, estadoEquipo(equipo));
        assertEquals(0, contarMovimientos(equipo));
    }

    @Test
    void validaContratoEstrictoIdsMotivoYTamanosSinAceptarOrigenActorTipoOFechaDelCliente() throws Exception {
        int equipo = nuevoEquipo("VALIDAR", "OPERATIVO");
        Map<String,Object> original = estadoEquipo(equipo);
        for (String bad : List.of("{", "null", "{}", "{\"motivo\":\"Motivo\"}", "{\"idLaboratorioDestino\":" + labB + "}")) {
            error(400, enviar("POST", trasladoRuta(equipo), jwtAdmin, bad));
        }
        for (Object badId : new Object[] { null, 0, -1, "abc" }) {
            Map<String,Object> body = request(labB, "Motivo"); body.put("idLaboratorioDestino", badId);
            error(400, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(body)));
        }
        for (Object motive : new Object[] { null, "", "   ", "x".repeat(501), List.of("no es texto") }) {
            Map<String,Object> body = request(labB, "Motivo"); body.put("motivo", motive);
            error(400, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(body)));
        }
        Map<String,Object> longLocation = request(labB, "Motivo"); longLocation.put("ubicacionInternaDestino", "x".repeat(201));
        error(400, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(longLocation)));
        for (String field : List.of("idLaboratorioOrigen", "idUsuarioActor", "tipoMovimiento", "fechaMovimiento", "idEquipo", "estado", "responsable", "idResponsable")) {
            Map<String,Object> body = request(labB, "Motivo"); body.put(field, "valor no autorizado");
            error(400, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(body)));
        }
        for (String invalid : List.of("0", "-1", "abc", "2147483648")) {
            error(400, enviar("POST", "/api/equipos/" + invalid + "/traslados", jwtAdmin, json(request(labB, "Motivo"))));
            error(400, enviar("GET", "/api/equipos/" + invalid + "/movimientos", jwtAdmin, null));
            error(400, enviar("GET", "/api/movimientos?idLaboratorio=" + invalid, jwtAdmin, null));
        }
        assertEquals(original, estadoEquipo(equipo));
        assertEquals(0, contarMovimientos(equipo));
    }

    @Test
    void historialYListaAplicanAlcanceActualSobreOrigenODestinoAunqueEquipoYaEsteFuera() throws Exception {
        int equipo = nuevoEquipo("HISTORIAL", "OPERATIVO");
        int first = trasladar(equipo, labB, jwtAdmin, "A hacia B", null).path("movimiento").path("id").asInt();
        int second = trasladar(equipo, labC, jwtAdmin, "B hacia C", null).path("movimiento").path("id").asInt();
        assertIds(get(historialRuta(equipo), jwtAdmin), first, second);
        assertTrue(ids(get("/api/movimientos", jwtAdmin)).containsAll(List.of(first,second)));
        for (int user : new int[] { gestor, lector }) {
            String jwt = user == gestor ? jwtGestor : jwtLector;
            asignar(user, labA);
            error(403, enviar("GET", "/api/equipos/" + equipo, jwt, null));
            assertIds(get(historialRuta(equipo), jwt), first);
            assertIds(get("/api/movimientos", jwt), first);
            assertIds(get("/api/movimientos?idLaboratorio=" + labA, jwt), first);
            error(403, enviar("GET", "/api/movimientos?idLaboratorio=" + labB, jwt, null));
            asignar(user, labB);
            assertIds(get(historialRuta(equipo), jwt), first, second);
            assertIds(get("/api/movimientos?idLaboratorio=" + labB, jwt), first, second);
            asignar(user, labC);
            assertIds(get(historialRuta(equipo), jwt), second);
            assertIds(get("/api/movimientos", jwt), second);
            asignar(user);
            assertIds(get("/api/movimientos", jwt));
            error(403, enviar("GET", historialRuta(equipo), jwt, null));
        }
        assertIds(get("/api/movimientos?idLaboratorio=" + labA, jwtAdmin), first);
        assertIds(get("/api/movimientos?idLaboratorio=" + labB, jwtAdmin), first, second);
        assertIds(get("/api/movimientos?idLaboratorio=" + labC, jwtAdmin), second);
    }

    @Test
    void historialVacioDistingueEquipoDentroDeAlcanceFueraDeAlcanceEInexistente() throws Exception {
        int equipo = nuevoEquipo("VACIO", "OPERATIVO");
        assertIds(get(historialRuta(equipo), jwtAdmin));
        assertIds(get(historialRuta(equipo), jwtGestor));
        assertIds(get(historialRuta(equipo), jwtLector));
        asignar(lector, labC);
        error(403, enviar("GET", historialRuta(equipo), jwtLector, null));
        for (String jwt : List.of(jwtAdmin, jwtGestor, jwtLector)) error(404, enviar("GET", historialRuta(Integer.MAX_VALUE), jwt, null));
    }

    @Test
    void ordenEsFechaDescendenteEIdDescendenteComoDesempate() throws Exception {
        int equipo = nuevoEquipo("ORDEN", "OPERATIVO");
        int first = trasladar(equipo, labB, jwtAdmin, "Primero", null).path("movimiento").path("id").asInt();
        int second = trasladar(equipo, labC, jwtAdmin, "Segundo", null).path("movimiento").path("id").asInt();
        int third = trasladar(equipo, labA, jwtAdmin, "Tercero", null).path("movimiento").path("id").asInt();
        assertEquals(List.of(third,second,first), orderedIds(get(historialRuta(equipo), jwtAdmin)));
        // Fechas controladas exclusivamente en fixtures: verifica fecha primaria y empate por ID.
        jdbc.update("UPDATE movimiento_equipo SET fecha_movimiento=TIMESTAMPTZ '2026-01-01 00:00:00+00' WHERE id_equipo=?", equipo);
        jdbc.update("UPDATE movimiento_equipo SET fecha_movimiento=TIMESTAMPTZ '2026-01-02 00:00:00+00' WHERE id_movimiento=?", first);
        assertEquals(List.of(first,third,second), orderedIds(get(historialRuta(equipo), jwtAdmin)));
        assertEquals(List.of(first,third), orderedIds(get("/api/movimientos?idLaboratorio=" + labA, jwtAdmin)));
    }

    @Test
    void historialSobreviveBajaDeEquipoLaboratorioHistoricoYActorInactivo() throws Exception {
        int equipo = nuevoEquipo("PERSISTENCIA", "OPERATIVO");
        int movement = trasladar(equipo, labB, jwtGestor, "Histórico", null).path("movimiento").path("id").asInt();
        assertEquals(204, enviar("DELETE", "/api/equipos/" + equipo, jwtAdmin, null).statusCode());
        assertIds(get(historialRuta(equipo), jwtLector), movement);
        asignar(gestor); asignar(lector);
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labA, jwtAdmin, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labB, jwtAdmin, null).statusCode());
        jdbc.update("UPDATE usuario SET activo=FALSE WHERE id_usuario=?", gestor);
        JsonNode history = get(historialRuta(equipo), jwtAdmin);
        assertIds(history, movement);
        assertEquals(gestor, history.get(0).path("actor").path("id").asInt());
        assertEquals(labA, history.get(0).path("laboratorioOrigen").path("id").asInt());
        assertIds(get("/api/movimientos?idLaboratorio=" + labB, jwtAdmin), movement);
        assertEquals(1, contarMovimientos(equipo));
        error(409, enviar("POST", trasladoRuta(equipo), jwtAdmin, json(request(labC, "No reactivar"))));
    }

    @Test
    void adminPuedeRepararOrigenInactivoPeroGestorNecesitaOrigenActivo() throws Exception {
        int equipo = nuevoEquipo("LEGACY", "OPERATIVO");
        jdbc.update("UPDATE laboratorio SET activo=FALSE WHERE id_laboratorio=?", labA);
        try {
            Map<String,Object> original = estadoEquipo(equipo);
            error(403, enviar("POST", trasladoRuta(equipo), jwtGestor, json(request(labB, "Origen inactivo"))));
            assertEquals(original, estadoEquipo(equipo));
            assertEquals(0, contarMovimientos(equipo));
            JsonNode repaired = trasladar(equipo, labB, jwtAdmin, "Reparación autorizada", null);
            assertEquals(labA, repaired.path("movimiento").path("laboratorioOrigen").path("id").asInt());
            assertEquals(labB, repaired.path("equipo").path("laboratorio").path("id").asInt());
        } finally {
            jdbc.update("UPDATE laboratorio SET activo=TRUE WHERE id_laboratorio=?", labA);
        }
    }

    @Test
    void origenNullableDeV3NoOcultaUnRegistroHistoricoVisiblePorDestino() throws Exception {
        int equipo = nuevoEquipo("ORIGEN-NULL", "OPERATIVO");
        int movement = jdbc.queryForObject("INSERT INTO movimiento_equipo(id_equipo,id_laboratorio_origen,id_laboratorio_destino,id_usuario_actor,tipo_movimiento,motivo) "
                + "VALUES (?,NULL,?,?,'TRASLADO',?) RETURNING id_movimiento", Integer.class, equipo, labA, admin, "Fixture válida V3 con origen desconocido");
        for (String jwt : List.of(jwtAdmin, jwtGestor, jwtLector)) {
            JsonNode history = get(historialRuta(equipo), jwt);
            assertIds(history, movement);
            assertTrue(history.get(0).path("laboratorioOrigen").isNull());
            assertIds(get("/api/movimientos?idLaboratorio=" + labA, jwt), movement);
        }
    }

    @Test
    void jwtProtegeLosEndpointsYLaApiNoPermiteCrearEditarNiBorrarMovimientosDirectamente() throws Exception {
        int equipo = nuevoEquipo("SEGURIDAD", "OPERATIVO");
        for (String jwt : new String[] { null, "token-invalido" }) {
            error(401, enviar("POST", trasladoRuta(equipo), jwt, json(request(labB, "Sin autenticación"))));
            error(401, enviar("GET", historialRuta(equipo), jwt, null));
            error(401, enviar("GET", "/api/movimientos", jwt, null));
        }
        error(403, enviar("POST", trasladoRuta(equipo), jwtLector, "{}"));
        int movement = trasladar(equipo, labB, jwtAdmin, "Evento inmutable", null).path("movimiento").path("id").asInt();
        List<Map<String,Object>> before = jdbc.queryForList("SELECT * FROM movimiento_equipo WHERE id_equipo=?", equipo);
        error(403, enviar("POST", "/api/movimientos", jwtAdmin, "{}"));
        error(403, enviar("PUT", "/api/movimientos/" + movement, jwtAdmin, "{}"));
        error(403, enviar("DELETE", "/api/movimientos/" + movement, jwtAdmin, null));
        assertEquals(before, jdbc.queryForList("SELECT * FROM movimiento_equipo WHERE id_equipo=?", equipo));
    }

    private int nuevoLab(String label) {
        return jdbc.queryForObject("INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio", Integer.class, "S6 " + label, "S6-" + label + "-" + run, area);
    }
    private int nuevoUsuario(String role) {
        int id = jdbc.queryForObject("INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol) "
                + "SELECT ?,?,?,?,seed.password_hash,r.id_rol FROM usuario seed JOIN rol r ON r.nombre=? WHERE seed.username='marko' RETURNING id_usuario",
                Integer.class, username(role), "Prueba", "Movimiento", username(role) + "@example.invalid", role);
        usuariosPropios.add(id); return id;
    }
    private int nuevoEquipo(String label, String state) {
        return jdbc.queryForObject("INSERT INTO equipo(codigo_interno,nombre,estado,id_subcategoria,id_laboratorio,id_responsable,ubicacion_interna) VALUES (?,?,?,?,?,?,?) RETURNING id_equipo",
                Integer.class, "S6-" + label + "-" + run, "Equipo " + label, state, subcategoria, labA, lector, "Armario del origen");
    }
    private String username(String role) { return "s6_" + role.toLowerCase(java.util.Locale.ROOT) + "_" + run; }
    private String token(String role) { return jwtService.generateToken(usuarioService.loadUserByUsername(username(role))); }
    private String trasladoRuta(int equipo) { return "/api/equipos/" + equipo + "/traslados"; }
    private String historialRuta(int equipo) { return "/api/equipos/" + equipo + "/movimientos"; }
    private Map<String,Object> request(int destination, String motive) { return new LinkedHashMap<>(Map.of("idLaboratorioDestino", destination, "motivo", motive)); }
    private String json(Map<String,Object> body) { return mapper.writeValueAsString(body); }
    private JsonNode trasladar(int equipo, int destination, String jwt, String motive, String location) throws Exception {
        Map<String,Object> body = request(destination, motive);
        body.put("ubicacionInternaDestino", location);
        return ok(200, enviar("POST", trasladoRuta(equipo), jwt, json(body)));
    }
    private void asignar(int user, Integer... labs) throws Exception {
        ok(200, enviar("PUT", "/api/admin/usuarios/" + user + "/laboratorios", jwtAdmin,
                mapper.writeValueAsString(Map.of("idsLaboratorio", List.of(labs)))));
    }
    private int contarMovimientos(int equipo) { return jdbc.queryForObject("SELECT count(*) FROM movimiento_equipo WHERE id_equipo=?", Integer.class, equipo); }
    private Map<String,Object> estadoEquipo(int equipo) { return jdbc.queryForMap("SELECT id_laboratorio,ubicacion_interna,estado,fecha_actualizacion FROM equipo WHERE id_equipo=?", equipo); }
    private JsonNode get(String path, String jwt) throws Exception { return ok(200, enviar("GET", path, jwt, null)); }
    private HttpResponse<String> enviar(String method, String path, String jwt, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode ok(int status, HttpResponse<String> response) {
        assertEquals(status, response.statusCode(), response.body());
        for (String secret : List.of("passwordHash", "password_hash", "$2a$", "$2b$", "JWT_SECRET")) assertFalse(response.body().contains(secret));
        return mapper.readTree(response.body());
    }
    private void error(int status, HttpResponse<String> response) {
        JsonNode error = ok(status, response);
        assertEquals(status, error.path("status").asInt());
        assertFalse(error.path("message").asString().isBlank());
        assertFalse(error.has("trace"));
        assertFalse(response.body().contains("org.hibernate"));
        assertFalse(response.body().contains("fk_movimiento"));
    }
    private TreeSet<Integer> ids(JsonNode rows) {
        assertTrue(rows.isArray());
        TreeSet<Integer> ids = new TreeSet<>();
        for (JsonNode row : rows) assertTrue(ids.add(row.path("id").asInt()));
        return ids;
    }
    private List<Integer> orderedIds(JsonNode rows) {
        List<Integer> ids = new ArrayList<>();
        rows.forEach(row -> ids.add(row.path("id").asInt()));
        return ids;
    }
    private void assertIds(JsonNode rows, Integer... expected) { assertEquals(new TreeSet<>(List.of(expected)), ids(rows)); }
}
