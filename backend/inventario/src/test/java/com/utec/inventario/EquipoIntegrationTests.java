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
class EquipoIntegrationTests {
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
    private int subA;
    private int subB;
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
    void prepararFixturesPropios() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        run = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede", Integer.class, "S5 " + run);
        area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area", Integer.class, "S5 " + run, sede);
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria", Integer.class, "S5 " + run);
        subA = jdbc.queryForObject("INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria", Integer.class, "A " + run, categoria);
        subB = jdbc.queryForObject("INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria", Integer.class, "B " + run, categoria);
        labA = nuevoLab("A");
        labB = nuevoLab("B");
        admin = nuevoUsuario("ADMIN");
        gestor = nuevoUsuario("GESTOR");
        lector = nuevoUsuario("LECTOR");
        jwtAdmin = token("ADMIN");
        jwtGestor = token("GESTOR");
        jwtLector = token("LECTOR");
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?),(?,?)", gestor, labA, lector, labA);
    }

    @AfterEach
    void limpiarSoloFixturesPropiosEnBaseTemporal() {
        if (client != null) client.close();
        jdbc.update("DELETE FROM equipo WHERE id_laboratorio IN (?,?)", labA, labB);
        for (int user : usuariosPropios) {
            jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario = ?", user);
            jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", user);
        }
        jdbc.update("DELETE FROM laboratorio WHERE id_area = ?", area);
        jdbc.update("DELETE FROM area WHERE id_area = ?", area);
        jdbc.update("DELETE FROM sede WHERE id_sede = ?", sede);
        jdbc.update("DELETE FROM subcategoria WHERE id_categoria = ?", categoria);
        jdbc.update("DELETE FROM categoria WHERE id_categoria = ?", categoria);
    }

    @Test
    void adminCompletaCrudConRelacionesFechasYBajaConsultableSinBorradoFisico() throws Exception {
        Map<String,Object> body = create("CRUD", labA, subA);
        body.putAll(Map.of("nombre", "  Osciloscopio  ", "marca", "  Rigol  ", "modelo", "  DS1054Z  ",
                "anio", 2025, "idResponsable", lector, "serieUtec", "SU-" + run, "numeroSerie", "SN-" + run));
        HttpResponse<String> created = enviar("POST", "/api/equipos", jwtAdmin, json(body));
        JsonNode original = ok(201, created);
        int id = original.path("id").asInt();
        String path = equipo(id);
        assertTrue(created.headers().firstValue("Location").orElse("").endsWith(path));
        assertEquals("Osciloscopio", original.path("nombre").asString());
        assertEquals("Rigol", original.path("marca").asString());
        assertEquals("DS1054Z", original.path("modelo").asString());
        assertEquals(subA, original.path("subcategoria").path("id").asInt());
        assertEquals(labA, original.path("laboratorio").path("id").asInt());
        assertEquals(lector, original.path("responsable").path("id").asInt());
        assertEquals(2, original.path("subcategoria").size());
        assertEquals(3, original.path("laboratorio").size());
        assertEquals(4, original.path("responsable").size());
        assertEquals(original, get(path, jwtAdmin));
        assertTrue(ids(get("/api/equipos", jwtAdmin)).contains(id));

        Map<String,Object> change = update(subB);
        change.put("estado", "MANTENIMIENTO");
        change.put("idResponsable", gestor);
        JsonNode updated = ok(200, enviar("PUT", path, jwtAdmin, json(change)));
        assertImmutable(original, updated);
        assertEquals(subB, updated.path("subcategoria").path("id").asInt());
        assertEquals(gestor, updated.path("responsable").path("id").asInt());
        for (String field : List.of("serieUtec", "numeroSerie", "marca", "modelo", "anio", "ordenCompra", "ubicacionInterna", "comentario")) {
            assertTrue(updated.path(field).isNull(), field + " omitido en PUT queda null");
        }
        assertAfter(original, updated);
        HttpResponse<String> deleted = enviar("DELETE", path, jwtAdmin, null);
        assertEquals(204, deleted.statusCode(), deleted.body());
        assertTrue(deleted.body().isEmpty());
        JsonNode baja = get(path, jwtAdmin);
        assertEquals("BAJA", baja.path("estado").asString());
        assertImmutable(original, baja);
        assertAfter(updated, baja);
        assertEquals("BAJA", jdbc.queryForObject("SELECT estado FROM equipo WHERE id_equipo = ?", String.class, id));
        error(409, enviar("DELETE", path, jwtAdmin, null));
        error(409, enviar("PUT", path, jwtAdmin, json(update(subB))));
        assertTrue(ids(get("/api/equipos?estado=BAJA", jwtAdmin)).contains(id));
    }

    @Test
    void gestorAdministraSoloDentroDeAlcanceYLectorNoEscribe() throws Exception {
        int inside = crear("GESTOR", labA, subA, jwtGestor);
        int outside = crear("FUERA", labB, subB, jwtAdmin);
        assertEquals(inside, get(equipo(inside), jwtGestor).path("id").asInt());
        assertEquals(inside, get(equipo(inside), jwtLector).path("id").asInt());
        ok(200, enviar("PUT", equipo(inside), jwtGestor, json(update(subB))));
        for (String jwt : List.of(jwtGestor, jwtLector)) error(403, enviar("GET", equipo(outside), jwt, null));
        error(403, enviar("POST", "/api/equipos", jwtGestor, json(create("NO", labB, subA))));
        error(403, enviar("PUT", equipo(outside), jwtGestor, json(update(subA))));
        error(403, enviar("DELETE", equipo(outside), jwtGestor, null));
        error(403, enviar("POST", "/api/equipos", jwtLector, json(create("LECTOR", labA, subA))));
        error(403, enviar("PUT", equipo(inside), jwtLector, json(update(subA))));
        error(403, enviar("DELETE", equipo(inside), jwtLector, null));
        assertEquals(204, enviar("DELETE", equipo(inside), jwtGestor, null).statusCode());
        assertEquals("BAJA", get(equipo(inside), jwtGestor).path("estado").asString());
        assertEquals("BAJA", get(equipo(inside), jwtLector).path("estado").asString());
        assertEquals("OPERATIVO", get(equipo(outside), jwtAdmin).path("estado").asString());
    }

    @Test
    void listadosYFiltrosSeCombinanSinExponerEquiposFueraDeAlcance() throws Exception {
        Map<String,Object> one = create("UNO", labA, subA);
        one.put("requiereMantenimiento", true);
        int a = crear(one, jwtAdmin);
        Map<String,Object> two = create("DOS", labA, subB);
        two.put("estado", "MANTENIMIENTO");
        int b = crear(two, jwtAdmin);
        Map<String,Object> three = create("TRES", labB, subA);
        three.put("estado", "INOPERATIVO");
        three.put("requiereMantenimiento", true);
        int c = crear(three, jwtAdmin);
        int d = crear("CUATRO", labA, subA, jwtAdmin);
        assertEquals(204, enviar("DELETE", equipo(d), jwtAdmin, null).statusCode());
        assertTrue(ids(get("/api/equipos", jwtAdmin)).containsAll(List.of(a,b,c,d)));
        assertIds(get("/api/equipos?idLaboratorio=" + labB, jwtAdmin), c);
        assertIds(get("/api/equipos?idSubcategoria=" + subB, jwtAdmin), b);
        assertIds(get("/api/equipos?idSubcategoria=" + subA + "&requiereMantenimiento=true", jwtAdmin), a,c);
        assertIds(get("/api/equipos?idLaboratorio=" + labA + "&estado=OPERATIVO&requiereMantenimiento=true&idSubcategoria=" + subA, jwtAdmin), a);
        for (String jwt : List.of(jwtGestor, jwtLector)) {
            assertIds(get("/api/equipos", jwt), a,b,d);
            assertIds(get("/api/equipos?estado=BAJA", jwt), d);
            assertIds(get("/api/equipos?requiereMantenimiento=true", jwt), a);
            assertIds(get("/api/equipos?idSubcategoria=" + subA, jwt), a,d);
            assertIds(get("/api/equipos?estado=MANTENIMIENTO&requiereMantenimiento=false", jwt), b);
            assertIds(get("/api/equipos?idLaboratorio=" + labA + "&idSubcategoria=" + subB, jwt), b);
            error(403, enviar("GET", "/api/equipos?idLaboratorio=" + labB + "&estado=BAJA", jwt, null));
        }
    }

    @Test
    void revocarAsignacionConElMismoJwtVacíaListadoYRevocaDetalleYEscritura() throws Exception {
        int id = crear("REVOCAR", labA, subA, jwtGestor);
        for (int user : new int[] { gestor, lector }) {
            String jwt = user == gestor ? jwtGestor : jwtLector;
            ok(200, enviar("PUT", "/api/admin/usuarios/" + user + "/laboratorios", jwtAdmin, "{\"idsLaboratorio\":[]}"));
            assertIds(get("/api/equipos", jwt));
            error(403, enviar("GET", equipo(id), jwt, null));
            error(403, enviar("GET", "/api/equipos?idLaboratorio=" + labA, jwt, null));
        }
        error(403, enviar("POST", "/api/equipos", jwtGestor, json(create("REVOCADO", labA, subA))));
        error(403, enviar("PUT", equipo(id), jwtGestor, json(update(subA))));
        error(403, enviar("DELETE", equipo(id), jwtGestor, null));
    }

    @Test
    void identificadoresUnicosIncluyenEquiposBajaYRespetanMayusculasDeV3() throws Exception {
        Map<String,Object> original = create("DUPLICADO", labA, subA);
        original.put("serieUtec", "SU-" + run);
        original.put("numeroSerie", "SN-" + run);
        int first = crear(original, jwtAdmin);
        int second = crear("OTRO", labB, subB, jwtAdmin);
        for (boolean afterBaja : new boolean[] { false, true }) {
            if (afterBaja) assertEquals(204, enviar("DELETE", equipo(first), jwtAdmin, null).statusCode());
            for (String field : List.of("codigoInterno", "serieUtec", "numeroSerie")) {
                Map<String,Object> duplicate = create("INTENTO-" + field, labB, subB);
                duplicate.put(field, original.get(field));
                error(409, enviar("POST", "/api/equipos", jwtAdmin, json(duplicate)));
                if (!field.equals("codigoInterno")) {
                    Map<String,Object> change = update(subB);
                    change.put(field, original.get(field));
                    error(409, enviar("PUT", equipo(second), jwtAdmin, json(change)));
                }
            }
        }
        Map<String,Object> own = update(subA);
        // Otro equipo puede mantener sus propias series al actualizar; se excluye su propio ID.
        own.put("serieUtec", "OWN-SU-" + run);
        own.put("numeroSerie", "OWN-SN-" + run);
        ok(200, enviar("PUT", equipo(second), jwtAdmin, json(own)));
        ok(200, enviar("PUT", equipo(second), jwtAdmin, json(own)));
        Map<String,Object> caseDifferent = create("CASE", labA, subA);
        for (String field : List.of("codigoInterno", "serieUtec", "numeroSerie")) {
            caseDifferent.put(field, ((String)original.get(field)).toLowerCase(java.util.Locale.ROOT));
        }
        crear(caseDifferent, jwtAdmin);
    }

    @Test
    void opcionalesVaciosSeNormalizanANullYNoChocanEntreSi() throws Exception {
        List<String> optional = List.of("serieUtec", "numeroSerie", "marca", "modelo", "ordenCompra", "ubicacionInterna", "comentario");
        for (int i = 0; i < 2; i++) {
            Map<String,Object> request = create("NULOS-" + i, labA, subA);
            for (String field : optional) request.put(field, i == 0 ? "   " : "");
            JsonNode response = get(equipo(crear(request, jwtAdmin)), jwtAdmin);
            for (String field : optional) assertTrue(response.path(field).isNull(), field);
            assertTrue(response.path("responsable").isNull());
        }
        Map<String,Object> text = create("TEXTO", labA, subA);
        text.put("comentario", "  " + "x".repeat(1200) + "  ");
        JsonNode response = get(equipo(crear(text, jwtAdmin)), jwtAdmin);
        assertEquals("x".repeat(1200), response.path("comentario").asString(), "V3 usa TEXT sin límite varchar inventado");
    }

    @Test
    void referenciasInexistentesOInactivasSeRechazanSinCrearNiModificar() throws Exception {
        int id = crear("REFERENCIAS", labA, subA, jwtAdmin);
        assertEquals(204, enviar("DELETE", "/api/subcategorias/" + subB, jwtAdmin, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labB, jwtAdmin, null).statusCode());
        jdbc.update("UPDATE usuario SET activo=FALSE WHERE id_usuario=?", lector);
        String[] fields = { "idSubcategoria", "idLaboratorio", "idResponsable" };
        int[] inactive = { subB, labB, lector };
        for (int i = 0; i < fields.length; i++) {
            Map<String,Object> missing = create("FALTA-" + i, labA, subA);
            missing.put(fields[i], Integer.MAX_VALUE);
            error(404, enviar("POST", "/api/equipos", jwtAdmin, json(missing)));
            Map<String,Object> disabled = create("INACTIVO-" + i, labA, subA);
            disabled.put(fields[i], inactive[i]);
            error(409, enviar("POST", "/api/equipos", jwtAdmin, json(disabled)));
            if (!fields[i].equals("idLaboratorio")) {
                Map<String,Object> change = update(subA);
                change.put(fields[i], Integer.MAX_VALUE);
                error(404, enviar("PUT", equipo(id), jwtAdmin, json(change)));
                change.put(fields[i], inactive[i]);
                error(409, enviar("PUT", equipo(id), jwtAdmin, json(change)));
            }
        }
        assertEquals(subA, get(equipo(id), jwtAdmin).path("subcategoria").path("id").asInt());
        assertEquals(1, contarEquipos());
    }

    @Test
    void responsabilidadNoConcedeAlcanceYPuedeConservarseSiElUsuarioLuegoSeInactiva() throws Exception {
        Map<String,Object> request = create("RESPONSABLE", labB, subA);
        request.put("idResponsable", lector); // LECTOR solo tiene alcance en A.
        int id = crear(request, jwtAdmin);
        error(403, enviar("GET", equipo(id), jwtLector, null));
        assertFalse(ids(get("/api/equipos", jwtLector)).contains(id));
        jdbc.update("UPDATE usuario SET activo=FALSE WHERE id_usuario=?", lector);
        Map<String,Object> preserve = update(subA);
        preserve.put("idResponsable", lector);
        JsonNode updated = ok(200, enviar("PUT", equipo(id), jwtAdmin, json(preserve)));
        assertEquals(lector, updated.path("responsable").path("id").asInt());
        assertTrue(ok(200, enviar("PUT", equipo(id), jwtAdmin, json(update(subA)))).path("responsable").isNull());
        error(409, enviar("PUT", equipo(id), jwtAdmin, json(preserve)));
    }

    @Test
    void putRechazaCamposInmutablesYNoPermiteTrasladoNiBajaEncubierta() throws Exception {
        int id = crear("INMUTABLE", labA, subA, jwtAdmin);
        JsonNode original = get(equipo(id), jwtAdmin);
        Map<String,Object> forbidden = Map.of("id", 999999, "idEquipo", 999999,
                "codigoInterno", "CAMBIO", "idLaboratorio", labB,
                "fechaCreacion", "2000-01-01T00:00:00Z", "fechaActualizacion", "2000-01-01T00:00:00Z");
        for (var entry : forbidden.entrySet()) {
            Map<String,Object> request = update(subA);
            request.put(entry.getKey(), entry.getValue());
            HttpResponse<String> response = enviar("PUT", equipo(id), jwtAdmin, json(request));
            error(400, response);
            if (entry.getKey().equals("idLaboratorio")) assertTrue(response.body().toLowerCase().contains("traslado"));
            assertEquals(original, get(equipo(id), jwtAdmin));
        }
        Map<String,Object> baja = update(subA);
        baja.put("estado", "BAJA");
        error(409, enviar("PUT", equipo(id), jwtAdmin, json(baja)));
        Map<String,Object> createBaja = create("ALTA-BAJA", labA, subA);
        createBaja.put("estado", "BAJA");
        error(409, enviar("POST", "/api/equipos", jwtAdmin, json(createBaja)));
        assertEquals(original, get(equipo(id), jwtAdmin));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM movimiento_equipo WHERE id_equipo=?", Integer.class, id));
    }

    @Test
    void padresConEquiposNoBajaBloqueanBajaYElHistoricoAdminSobreviveALosPadresInactivos() throws Exception {
        int id = crear("PADRES", labB, subB, jwtAdmin);
        error(409, enviar("DELETE", "/api/subcategorias/" + subB, jwtAdmin, null));
        error(409, enviar("DELETE", "/api/laboratorios/" + labB, jwtAdmin, null));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT activo FROM subcategoria WHERE id_subcategoria=?", Boolean.class, subB));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT activo FROM laboratorio WHERE id_laboratorio=?", Boolean.class, labB));
        assertEquals(204, enviar("DELETE", equipo(id), jwtAdmin, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/subcategorias/" + subB, jwtAdmin, null).statusCode());
        assertEquals(204, enviar("DELETE", "/api/laboratorios/" + labB, jwtAdmin, null).statusCode());
        assertEquals("BAJA", get(equipo(id), jwtAdmin).path("estado").asString());
        assertIds(get("/api/equipos?idLaboratorio=" + labB + "&estado=BAJA", jwtAdmin), id);
        assertIds(get("/api/admin/equipos?idLaboratorio=" + labB, jwtAdmin), id);
        error(403, enviar("GET", equipo(id), jwtGestor, null));
        error(409, enviar("DELETE", "/api/laboratorios/" + labA, jwtAdmin, null), "Las asignaciones de usuario todavía bloquean");
    }

    @Test
    void aliasAdminReutilizaFiltrosYSoloAdminAccede() throws Exception {
        int id = crear("ALIAS", labA, subA, jwtAdmin);
        String query = "?idSubcategoria=" + subA + "&estado=OPERATIVO&requiereMantenimiento=false";
        assertEquals(get("/api/equipos" + query, jwtAdmin), get("/api/admin/equipos" + query, jwtAdmin));
        assertIds(get("/api/admin/equipos" + query, jwtAdmin), id);
        for (String jwt : List.of(jwtGestor, jwtLector)) error(403, enviar("GET", "/api/admin/equipos", jwt, null));
    }

    @Test
    void sinJwtOJwtInvalidoTodasLasRutasEstanProtegidasEIdAusenteDa404() throws Exception {
        int id = crear("SEGURIDAD", labA, subA, jwtAdmin);
        for (String jwt : new String[] { null, "token-invalido" }) {
            for (String path : List.of("/api/equipos", equipo(id), "/api/admin/equipos")) error(401, enviar("GET", path, jwt, null));
            error(401, enviar("POST", "/api/equipos", jwt, json(create("SIN-JWT", labA, subA))));
            error(401, enviar("PUT", equipo(id), jwt, json(update(subA))));
            error(401, enviar("DELETE", equipo(id), jwt, null));
        }
        for (String jwt : List.of(jwtAdmin, jwtGestor, jwtLector)) error(404, enviar("GET", equipo(Integer.MAX_VALUE), jwt, null));
        error(404, enviar("PUT", equipo(Integer.MAX_VALUE), jwtAdmin, json(update(subA))));
        error(404, enviar("DELETE", equipo(Integer.MAX_VALUE), jwtAdmin, null));
    }

    @Test
    void validacionesDeCamposObligatoriosFormatosIdsYAnioSeAplicanAntesDePersistir() throws Exception {
        int id = crear("VALIDAR", labA, subA, jwtAdmin);
        for (String invalid : List.of("{", "{}", "null")) {
            error(400, enviar("POST", "/api/equipos", jwtAdmin, invalid));
            error(400, enviar("PUT", equipo(id), jwtAdmin, invalid));
        }
        Map<String,List<Object>> values = new LinkedHashMap<>();
        values.put("nombre", List.of("", "   "));
        values.put("estado", List.of("DESCONOCIDO", "operativo", "", 0));
        values.put("anio", List.of(1899, 2101, "abc"));
        values.put("idSubcategoria", List.of(0, -1, "abc"));
        values.put("idResponsable", List.of(0, -1, "abc"));
        values.put("requiereMantenimiento", List.of("abc"));
        for (var entry : values.entrySet()) for (Object value : entry.getValue()) {
            Map<String,Object> creation = create("INVALIDO", labA, subA);
            creation.put(entry.getKey(), value);
            error(400, enviar("POST", "/api/equipos", jwtAdmin, json(creation)));
            Map<String,Object> change = update(subA);
            change.put(entry.getKey(), value);
            error(400, enviar("PUT", equipo(id), jwtAdmin, json(change)));
        }
        for (String field : List.of("nombre", "estado", "requiereMantenimiento", "idSubcategoria", "codigoInterno", "idLaboratorio")) {
            Map<String,Object> creation = create("OMITIDO", labA, subA);
            creation.remove(field);
            error(400, enviar("POST", "/api/equipos", jwtAdmin, json(creation)));
            creation.put(field, null);
            error(400, enviar("POST", "/api/equipos", jwtAdmin, json(creation)));
            if (!field.equals("codigoInterno") && !field.equals("idLaboratorio")) {
                Map<String,Object> change = update(subA);
                change.remove(field);
                error(400, enviar("PUT", equipo(id), jwtAdmin, json(change)));
            }
        }
        for (Object invalidLab : List.of(0, -1, "abc")) {
            Map<String,Object> body = create("LAB-INVALIDO", labA, subA);
            body.put("idLaboratorio", invalidLab);
            error(400, enviar("POST", "/api/equipos", jwtAdmin, json(body)));
        }
        for (String blankCode : List.of("", "   ")) {
            Map<String,Object> body = create("CODIGO-VACIO", labA, subA);
            body.put("codigoInterno", blankCode);
            error(400, enviar("POST", "/api/equipos", jwtAdmin, json(body)));
        }
        for (int boundary : new int[] { 1900, 2100 }) {
            Map<String,Object> body = update(subA);
            body.put("anio", boundary);
            assertEquals(boundary, ok(200, enviar("PUT", equipo(id), jwtAdmin, json(body))).path("anio").asInt());
        }
        assertEquals(1, contarEquipos());
    }

    @Test
    void longitudesSeAjustanAV3YFiltrosInvalidosProducen400() throws Exception {
        int id = crear("LONGITUD", labA, subA, jwtAdmin);
        Map<String,Integer> limits = Map.of("codigoInterno", 50, "serieUtec", 100, "numeroSerie", 100,
                "nombre", 150, "marca", 100, "modelo", 100, "ordenCompra", 50, "ubicacionInterna", 200);
        for (var entry : limits.entrySet()) {
            Map<String,Object> body = create("LARGO", labA, subA);
            body.put(entry.getKey(), "x".repeat(entry.getValue() + 1));
            error(400, enviar("POST", "/api/equipos", jwtAdmin, json(body)));
            if (!entry.getKey().equals("codigoInterno")) {
                Map<String,Object> change = update(subA);
                change.put(entry.getKey(), "x".repeat(entry.getValue() + 1));
                error(400, enviar("PUT", equipo(id), jwtAdmin, json(change)));
            }
        }
        for (String invalid : List.of("0", "-1", "abc", "2147483648")) {
            error(400, enviar("GET", "/api/equipos/" + invalid, jwtAdmin, null));
            error(400, enviar("DELETE", "/api/equipos/" + invalid, jwtAdmin, null));
            error(400, enviar("GET", "/api/equipos?idLaboratorio=" + invalid, jwtAdmin, null));
            error(400, enviar("GET", "/api/equipos?idSubcategoria=" + invalid, jwtAdmin, null));
        }
        error(400, enviar("GET", "/api/equipos?estado=DESCONOCIDO", jwtAdmin, null));
        error(400, enviar("GET", "/api/equipos?requiereMantenimiento=abc", jwtAdmin, null));
        error(400, enviar("GET", "/api/admin/equipos?estado=DESCONOCIDO", jwtAdmin, null));
    }

    private int nuevoLab(String label) {
        return jdbc.queryForObject("INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio",
                Integer.class, "S5 " + label, "S5-" + label + "-" + run, area);
    }

    private int nuevoUsuario(String role) {
        int id = jdbc.queryForObject("INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol) "
                + "SELECT ?,?,?,?,seed.password_hash,r.id_rol FROM usuario seed JOIN rol r ON r.nombre=? "
                + "WHERE seed.username='marko' RETURNING id_usuario", Integer.class,
                username(role), "Prueba", "Equipo", username(role) + "@example.invalid", role);
        usuariosPropios.add(id);
        return id;
    }

    private String username(String role) { return "s5_" + role.toLowerCase(java.util.Locale.ROOT) + "_" + run; }
    private String token(String role) { return jwtService.generateToken(usuarioService.loadUserByUsername(username(role))); }
    private String equipo(int id) { return "/api/equipos/" + id; }
    private String json(Map<String,Object> body) { return mapper.writeValueAsString(body); }

    private Map<String,Object> create(String code, int lab, int sub) {
        Map<String,Object> result = update(sub);
        result.put("codigoInterno", "S5-" + code + "-" + run);
        result.put("idLaboratorio", lab);
        return result;
    }

    private Map<String,Object> update(int sub) {
        return new LinkedHashMap<>(Map.of("nombre", "Equipo " + run, "estado", "OPERATIVO",
                "requiereMantenimiento", false, "idSubcategoria", sub));
    }

    private int crear(String code, int lab, int sub, String jwt) throws Exception { return crear(create(code, lab, sub), jwt); }
    private int crear(Map<String,Object> body, String jwt) throws Exception { return ok(201, enviar("POST", "/api/equipos", jwt, json(body))).path("id").asInt(); }
    private JsonNode get(String path, String jwt) throws Exception { return ok(200, enviar("GET", path, jwt, null)); }
    private int contarEquipos() { return jdbc.queryForObject("SELECT count(*) FROM equipo WHERE id_laboratorio IN (?,?)", Integer.class, labA, labB); }

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
        for (String secret : List.of("passwordHash", "password_hash", "$2a$", "$2b$", "JWT_SECRET")) assertFalse(response.body().contains(secret));
        return mapper.readTree(response.body());
    }

    private void error(int status, HttpResponse<String> response, String... context) {
        JsonNode error = ok(status, response);
        assertEquals(status, error.path("status").asInt());
        assertFalse(error.path("message").asString().isBlank());
        assertFalse(error.has("trace"));
        assertFalse(response.body().contains("org.hibernate"));
        assertFalse(response.body().contains("uq_equipo"));
    }

    private TreeSet<Integer> ids(JsonNode rows) {
        assertTrue(rows.isArray());
        TreeSet<Integer> ids = new TreeSet<>();
        for (JsonNode row : rows) assertTrue(ids.add(row.path("id").asInt()));
        return ids;
    }

    private void assertIds(JsonNode rows, Integer... expected) { assertEquals(new TreeSet<>(List.of(expected)), ids(rows)); }

    private void assertImmutable(JsonNode original, JsonNode current) {
        for (String field : List.of("id", "codigoInterno", "fechaCreacion", "laboratorio")) assertEquals(original.path(field), current.path(field), field);
    }

    private void assertAfter(JsonNode previous, JsonNode next) {
        assertTrue(OffsetDateTime.parse(next.path("fechaActualizacion").asString()).toInstant()
                .isAfter(OffsetDateTime.parse(previous.path("fechaActualizacion").asString()).toInstant()));
    }
}
