package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class ReporteAuditoriaIntegrationTests {

    private static final List<String> REPORTES = List.of("/resumen", "/equipos/por-estado",
            "/equipos/por-laboratorio", "/movimientos", "/mantenimientos");
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarios;
    @Autowired private ObjectMapper mapper;
    @LocalServerPort private int port;

    private HttpClient client;
    private String run;
    private String admin;
    private String gestor;
    private String lector;
    private String vacio;
    private int adminId;
    private int gestorId;
    private int lectorId;
    private int vacioId;
    private int categoria;
    private int subcategoria;
    private int sedeA;
    private int sedeB;
    private int areaA;
    private int areaB;
    private int labA1;
    private int labA2;
    private int labB;
    private int equipoA1;
    private int equipoA1b;
    private int equipoA2;
    private int equipoB;
    private final List<Integer> usuariosPropios = new ArrayList<>();
    private final List<Integer> categoriasPropias = new ArrayList<>();

    @BeforeEach
    void prepararFixturesRealesEnBaseDeVerificacion() {
        client = HttpClient.newHttpClient();
        run = UUID.randomUUID().toString().substring(0, 18);
        admin = tokenDe("marko");
        adminId = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE username = 'marko'", Integer.class);
        gestorId = usuario("g", "GESTOR");
        lectorId = usuario("l", "LECTOR");
        vacioId = usuario("v", "LECTOR");
        gestor = tokenDe("rep_g_" + run);
        lector = tokenDe("rep_l_" + run);
        vacio = tokenDe("rep_v_" + run);
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria",
                Integer.class, "REP " + run);
        categoriasPropias.add(categoria);
        subcategoria = jdbc.queryForObject(
                "INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria",
                Integer.class, "REP " + run, categoria);
        sedeA = sede("A");
        sedeB = sede("B");
        areaA = area(sedeA, "A");
        areaB = area(sedeB, "B");
        labA1 = laboratorio(areaA, "A1");
        labA2 = laboratorio(areaA, "A2");
        labB = laboratorio(areaB, "B");
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?)", gestorId, labA1);
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?)", lectorId, labA2);
        equipoA1 = equipo(labA1, "A1", "OPERATIVO", "2026-09-10");
        equipoA1b = equipo(labA1, "A1b", "INOPERATIVO", "2026-09-12");
        equipoA2 = equipo(labA2, "A2", "BAJA", "2026-09-10");
        equipoB = equipo(labB, "B", "OPERATIVO", "2026-09-11");
        movimiento(equipoB, labA1, labB, "2026-09-10");
        movimiento(equipoA2, labB, labA2, "2026-09-11");
        movimiento(equipoA1, labA2, labA1, "2026-09-12");
        mantenimiento(equipoA1, "PREVENTIVO", "PROGRAMADO", "2026-09-15");
        mantenimiento(equipoA1b, "CORRECTIVO", "CANCELADO", "2026-09-16");
        mantenimiento(equipoA2, "CALIBRACION", "COMPLETADO", "2026-09-15");
        mantenimiento(equipoB, "PREVENTIVO", "PROGRAMADO", "2026-09-15");
    }

    @AfterEach
    void limpiarExclusivamenteFixturesPropios() {
        if (client != null) client.close();
        String equipos = "SELECT id_equipo FROM equipo WHERE id_laboratorio IN (?, ?, ?)";
        jdbc.update("DELETE FROM auditoria WHERE entidad = 'mantenimiento' AND id_entidad IN "
                + "(SELECT id_mantenimiento FROM mantenimiento WHERE id_equipo IN (" + equipos + "))",
                labA1, labA2, labB);
        jdbc.update("DELETE FROM auditoria WHERE entidad = 'equipo' AND id_entidad IN (" + equipos + ")",
                labA1, labA2, labB);
        for (int id : usuariosPropios) {
            jdbc.update("DELETE FROM auditoria WHERE id_usuario_actor = ? "
                    + "OR (entidad IN ('usuario', 'usuario_laboratorio') AND id_entidad = ?)", id, id);
        }
        for (int id : categoriasPropias) {
            jdbc.update("DELETE FROM auditoria WHERE entidad = 'categoria' AND id_entidad = ?", id);
        }
        jdbc.update("DELETE FROM mantenimiento WHERE id_equipo IN (" + equipos + ")", labA1, labA2, labB);
        jdbc.update("DELETE FROM movimiento_equipo WHERE id_equipo IN (" + equipos + ")", labA1, labA2, labB);
        jdbc.update("DELETE FROM equipo WHERE id_laboratorio IN (?, ?, ?)", labA1, labA2, labB);
        jdbc.update("DELETE FROM usuario_laboratorio WHERE id_laboratorio IN (?, ?, ?)", labA1, labA2, labB);
        jdbc.update("DELETE FROM laboratorio WHERE id_laboratorio IN (?, ?, ?)", labA1, labA2, labB);
        jdbc.update("DELETE FROM area WHERE id_area IN (?, ?)", areaA, areaB);
        jdbc.update("DELETE FROM sede WHERE id_sede IN (?, ?)", sedeA, sedeB);
        jdbc.update("DELETE FROM subcategoria WHERE id_subcategoria = ?", subcategoria);
        for (int id : categoriasPropias) jdbc.update("DELETE FROM categoria WHERE id_categoria = ?", id);
        for (int id : usuariosPropios) jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id);
        categoriasPropias.clear();
        usuariosPropios.clear();
    }

    @Test
    void resumenAdminAgregaDatosPersistidosIncluyendoBajaYTrasladoSinDuplicados() throws Exception {
        JsonNode resumen = get("/api/reportes/resumen?idSede=" + sedeA, admin);
        assertEquals(3, resumen.path("totalEquipos").asLong());
        assertEquals(3, resumen.path("totalMovimientos").asLong());
        assertEquals(3, resumen.path("totalMantenimientos").asLong());
        assertEquals(1, totalValor(resumen.path("equiposPorEstado"), "OPERATIVO"));
        assertEquals(1, totalValor(resumen.path("equiposPorEstado"), "INOPERATIVO"));
        assertEquals(1, totalValor(resumen.path("equiposPorEstado"), "BAJA"));
        assertEquals(1, totalValor(resumen.path("mantenimientosPorEstado"), "PROGRAMADO"));
        assertEquals(1, totalValor(resumen.path("mantenimientosPorEstado"), "COMPLETADO"));
        assertEquals(1, totalValor(resumen.path("mantenimientosPorEstado"), "CANCELADO"));
    }

    @Test
    void equiposFiltranJerarquiaEstadoYFechaDeCreacionConLimitesInclusivos() throws Exception {
        String filtros = "?idSede=" + sedeA + "&idArea=" + areaA + "&idLaboratorio=" + labA1
                + "&estado=OPERATIVO&fechaDesde=2026-09-10&fechaHasta=2026-09-10";
        JsonNode estados = get("/api/reportes/equipos/por-estado" + filtros, admin);
        assertEquals(1, estados.size());
        assertEquals(1, totalValor(estados, "OPERATIVO"));
        JsonNode labs = get("/api/reportes/equipos/por-laboratorio" + filtros, admin);
        assertEquals(1, labs.size());
        assertEquals(labA1, labs.get(0).path("idLaboratorio").asInt());
        assertEquals(1, labs.get(0).path("total").asLong());
        assertTrue(get("/api/reportes/equipos/por-estado?idSede=" + sedeB
                + "&idArea=" + areaA, admin).isEmpty());
        assertTrue(get("/api/reportes/equipos/por-estado?idLaboratorio=" + labA1
                + "&fechaDesde=2026-09-11&fechaHasta=2026-09-11", admin).isEmpty());
    }

    @Test
    void gestorYLectorAgreganUnicamenteSuAlcanceYNoPuedenSolicitarOtroLaboratorio() throws Exception {
        JsonNode reporteGestor = get("/api/reportes/resumen?idSede=" + sedeA, gestor);
        assertEquals(2, reporteGestor.path("totalEquipos").asLong());
        assertEquals(2, reporteGestor.path("totalMovimientos").asLong());
        assertEquals(2, reporteGestor.path("totalMantenimientos").asLong());
        JsonNode reporteLector = get("/api/reportes/resumen?idSede=" + sedeA, lector);
        assertEquals(1, reporteLector.path("totalEquipos").asLong());
        assertEquals(2, reporteLector.path("totalMovimientos").asLong());
        assertEquals(1, reporteLector.path("totalMantenimientos").asLong());
        JsonNode labsGestor = get("/api/reportes/equipos/por-laboratorio", gestor);
        JsonNode labsLector = get("/api/reportes/equipos/por-laboratorio", lector);
        assertEquals(1, labsGestor.size());
        assertEquals(labA1, labsGestor.get(0).path("idLaboratorio").asInt());
        assertEquals(1, labsLector.size());
        assertEquals(labA2, labsLector.get(0).path("idLaboratorio").asInt());
        for (String ruta : REPORTES) {
            assertEquals(403, enviar("GET", "/api/reportes" + ruta + "?idLaboratorio=" + labB,
                    gestor, null).statusCode());
            assertEquals(403, enviar("GET", "/api/reportes" + ruta + "?idLaboratorio=" + labB,
                    lector, null).statusCode());
            assertEquals(404, enviar("GET", "/api/reportes" + ruta + "?idLaboratorio=2147483647",
                    admin, null).statusCode());
        }
    }

    @Test
    void usuarioSinAsignacionesObtieneTotalesCeroYListasVacias() throws Exception {
        JsonNode resumen = get("/api/reportes/resumen", vacio);
        assertEquals(0, resumen.path("totalEquipos").asLong());
        assertEquals(0, resumen.path("totalMovimientos").asLong());
        assertEquals(0, resumen.path("totalMantenimientos").asLong());
        assertTrue(get("/api/reportes/equipos/por-estado", vacio).isEmpty());
        assertTrue(get("/api/reportes/equipos/por-laboratorio", vacio).isEmpty());
        assertEquals(0, get("/api/reportes/movimientos", vacio).path("total").asLong());
        assertEquals(0, get("/api/reportes/mantenimientos", vacio).path("total").asLong());
    }

    @Test
    void movimientosSonVisiblesPorOrigenODestinoAunqueEquipoEsteFueraDelAlcanceActual() throws Exception {
        assertEquals(2, get("/api/reportes/movimientos", gestor).path("total").asLong());
        assertEquals(2, get("/api/reportes/movimientos", lector).path("total").asLong());
        JsonNode ambos = get("/api/reportes/movimientos?idSede=" + sedeA, admin);
        assertEquals(3, ambos.path("total").asLong());
        assertEquals(3, totalValor(ambos.path("porTipo"), "TRASLADO"));
        assertEquals(1, get("/api/reportes/movimientos?fechaDesde=2026-09-10&fechaHasta=2026-09-10",
                gestor).path("total").asLong(), "El equipo de ese movimiento ahora está en B");
        assertEquals(1, get("/api/reportes/movimientos?idSede=" + sedeA + "&estado=BAJA",
                admin).path("total").asLong());
    }

    @Test
    void mantenimientosUsanFechaProgramadaYTienenFiltrosPorTipoEstadoYAlcance() throws Exception {
        String filtro = "?idSede=" + sedeA + "&idArea=" + areaA + "&idLaboratorio=" + labA1
                + "&fechaDesde=2026-09-15&fechaHasta=2026-09-15"
                + "&estadoMantenimiento=PROGRAMADO&tipoMantenimiento=PREVENTIVO";
        JsonNode reporte = get("/api/reportes/mantenimientos" + filtro, gestor);
        assertEquals(1, reporte.path("total").asLong());
        assertEquals(1, totalValor(reporte.path("porEstado"), "PROGRAMADO"));
        assertEquals(1, totalValor(reporte.path("porTipo"), "PREVENTIVO"));
        assertEquals(0, get("/api/reportes/mantenimientos?idLaboratorio=" + labA1
                + "&fechaDesde=2026-08-01&fechaHasta=2026-08-01",
                gestor).path("total").asLong(), "La fecha de creación no define el filtro de mantenimiento");
        assertEquals(1, get("/api/reportes/mantenimientos?idSede=" + sedeA
                + "&estadoMantenimiento=COMPLETADO&tipoMantenimiento=CALIBRACION",
                admin).path("total").asLong());
    }

    @Test
    void reportesYAuditoriaValidanJwtPermisosYParametrosSinExponerDetallesInternos() throws Exception {
        for (String ruta : REPORTES) {
            assertError(401, enviar("GET", "/api/reportes" + ruta, null, null));
            assertError(401, enviar("GET", "/api/reportes" + ruta, "token-invalido", null));
            assertError(400, enviar("GET", "/api/reportes" + ruta
                    + "?fechaDesde=2026-09-20&fechaHasta=2026-09-10", admin, null));
            assertError(400, enviar("GET", "/api/reportes" + ruta + "?idLaboratorio=0", admin, null));
            assertError(400, enviar("GET", "/api/reportes" + ruta + "?estado=DESCONOCIDO", admin, null));
        }
        assertError(401, enviar("GET", "/api/admin/auditoria", null, null));
        assertError(403, enviar("GET", "/api/admin/auditoria", gestor, null));
        assertError(403, enviar("GET", "/api/admin/auditoria", lector, null));
        assertError(400, enviar("GET", "/api/admin/auditoria?idEntidad=0", admin, null));
        assertError(400, enviar("GET", "/api/admin/auditoria?fechaDesde=2026-10-02&fechaHasta=2026-10-01",
                admin, null));
    }

    @Test
    void auditoriaDeCatalogoRegistraActorRealYCambiosYNoRegistraDuplicadosFallidos() throws Exception {
        String nombre = "REP-AUD-" + run;
        JsonNode creado = correcto(201, enviar("POST", "/api/categorias", admin,
                json(Map.of("nombre", nombre, "idUsuarioActor", gestorId, "rol", "LECTOR"))));
        int id = creado.path("id").asInt();
        categoriasPropias.add(id);
        correcto(200, enviar("PUT", "/api/categorias/" + id, admin, json(Map.of("nombre", nombre, "descripcion", "Editada"))));
        correcto(200, enviar("PATCH", "/api/categorias/" + id + "/estado", admin, "{\"activo\":false}"));
        correcto(200, enviar("PATCH", "/api/categorias/" + id + "/estado", admin, "{\"activo\":true}"));
        assertError(409, enviar("POST", "/api/categorias", admin, json(Map.of("nombre", nombre))));
        JsonNode eventos = eventos("categoria", id);
        assertEquals(4, eventos.size());
        assertEquals(List.of("ACTIVAR", "DESACTIVAR", "EDITAR", "CREAR"), acciones(eventos));
        comprobarActor(eventos, adminId, "marko");
        assertEquals(1, get("/api/admin/auditoria?entidad=categoria&idEntidad=" + id
                + "&accion=ACTIVAR&idUsuario=" + adminId, admin).size());
        var fechaEvento = OffsetDateTime.parse(eventos.get(0).path("fecha").asString())
                .withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        assertEquals(4, get("/api/admin/auditoria?entidad=categoria&idEntidad=" + id
                + "&fechaDesde=" + fechaEvento + "&fechaHasta=" + fechaEvento, admin).size());
        assertTrue(get("/api/admin/auditoria?entidad=categoria&idEntidad=" + id
                + "&fechaDesde=" + fechaEvento.plusDays(1), admin).isEmpty());
        comprobarSinSecretos(json(eventos), admin, gestor);
    }

    @Test
    void auditoriaDeEquipoRegistraCreacionEdicionTrasladoYBajaSoloCuandoOperacionTieneExito() throws Exception {
        JsonNode creado = correcto(201, enviar("POST", "/api/equipos", gestor, json(Map.of(
                "codigoInterno", "REP-API-" + run, "nombre", "Equipo auditoría", "estado", "OPERATIVO",
                "requiereMantenimiento", false, "idSubcategoria", subcategoria, "idLaboratorio", labA1))));
        int id = creado.path("id").asInt();
        correcto(200, enviar("PUT", "/api/equipos/" + id, gestor, json(Map.of(
                "nombre", "Equipo editado", "estado", "OPERATIVO", "requiereMantenimiento", false,
                "idSubcategoria", subcategoria))));
        assertError(403, enviar("POST", "/api/equipos/" + id + "/traslados", gestor,
                json(Map.of("idLaboratorioDestino", labB, "motivo", "No autorizado"))));
        assertError(409, enviar("POST", "/api/equipos/" + id + "/traslados", admin,
                json(Map.of("idLaboratorioDestino", labA1, "motivo", "Mismo origen y destino"))));
        correcto(200, enviar("POST", "/api/equipos/" + id + "/traslados", admin,
                json(Map.of("idLaboratorioDestino", labB, "motivo", "Traslado real"))));
        assertEquals(204, enviar("DELETE", "/api/equipos/" + id, admin, null).statusCode());
        JsonNode eventos = eventos("equipo", id);
        assertEquals(4, eventos.size());
        assertEquals(List.of("DESACTIVAR", "TRASLADAR", "EDITAR", "CREAR"), acciones(eventos));
        comprobarActor(get("/api/admin/auditoria?entidad=equipo&idEntidad=" + id
                + "&idUsuario=" + gestorId, admin), gestorId, "rep_g_" + run);
        assertEquals(2, get("/api/admin/auditoria?entidad=equipo&idEntidad=" + id
                + "&idUsuario=" + gestorId, admin).size());
        assertTrue(eventos.get(1).path("cambios").asString().contains(Integer.toString(labA1)));
        assertTrue(eventos.get(1).path("cambios").asString().contains(Integer.toString(labB)));
        comprobarSinSecretos(json(eventos), admin, gestor);
    }

    @Test
    void administracionDeUsuarioAuditaRolEstadoYPasswordSinGuardarSecretsEnEventos() throws Exception {
        String username = "rep_api_" + run;
        String inicial = "Inicial-" + run;
        String nueva = "Nueva-" + run;
        JsonNode creado = correcto(201, enviar("POST", "/api/admin/usuarios", admin, json(Map.of(
                "userName", username, "nombre", "Usuario", "apellido", "Auditoría",
                "email", username + "@example.test", "password", inicial, "rol", "GESTOR", "activo", true))));
        int id = creado.path("id").asInt();
        usuariosPropios.add(id);
        correcto(200, enviar("PATCH", "/api/admin/usuarios/" + id + "/rol", admin, "{\"rol\":\"LECTOR\"}"));
        assertEquals(204, enviar("PUT", "/api/admin/usuarios/" + id + "/password", admin,
                json(Map.of("password", nueva))).statusCode());
        correcto(200, enviar("PATCH", "/api/admin/usuarios/" + id + "/estado", admin, "{\"activo\":false}"));
        correcto(200, enviar("PATCH", "/api/admin/usuarios/" + id + "/estado", admin, "{\"activo\":true}"));
        assertError(400, enviar("PUT", "/api/admin/usuarios/" + id + "/password", admin,
                json(Map.of("password", "x"))));
        JsonNode eventos = eventos("usuario", id);
        assertEquals(5, eventos.size());
        comprobarActor(eventos, adminId, "marko");
        assertTrue(acciones(eventos).containsAll(List.of("CREAR", "CAMBIAR_ROL",
                "RESTABLECER_CONTRASENA", "DESACTIVAR", "ACTIVAR")));
        JsonNode reset = get("/api/admin/auditoria?entidad=usuario&idEntidad=" + id
                + "&accion=RESTABLECER_CONTRASENA", admin);
        assertEquals(1, reset.size());
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE id_usuario = ?", String.class, id);
        assertTrue(encoder.matches(nueva, hash));
        comprobarSinSecretos(json(eventos), inicial, nueva, hash, admin);
        comprobarSinSecretos(json(get("/api/admin/usuarios/" + id, admin)), inicial, nueva, hash);
    }

    @Test
    void asignacionesYMantenimientoAuditanEnMismaTransaccionYErroresNoCreanEventos() throws Exception {
        String asignacion = "/api/admin/usuarios/" + gestorId + "/laboratorios";
        String body = json(Map.of("idsLaboratorio", List.of(labA1, labA2)));
        correcto(200, enviar("PUT", asignacion, admin, body));
        correcto(200, enviar("PUT", asignacion, admin, body));
        JsonNode eventosAsignacion = eventos("usuario_laboratorio", gestorId);
        assertEquals(1, eventosAsignacion.size());
        assertEquals("ASIGNAR_LABORATORIOS", eventosAsignacion.get(0).path("accion").asString());
        comprobarActor(eventosAsignacion, adminId, "marko");
        JsonNode creado = correcto(201, enviar("POST", "/api/mantenimientos", gestor, json(Map.of(
                "idEquipo", equipoA1, "tipo", "PREVENTIVO", "descripcion", "Mantenimiento auditado",
                "fechaProgramada", "2026-10-10"))));
        int id = creado.path("id").asInt();
        correcto(200, enviar("PATCH", "/api/mantenimientos/" + id + "/estado", gestor,
                "{\"estado\":\"EN_PROCESO\"}"));
        correcto(200, enviar("PATCH", "/api/mantenimientos/" + id + "/estado", gestor,
                "{\"estado\":\"COMPLETADO\"}"));
        assertError(409, enviar("PATCH", "/api/mantenimientos/" + id + "/estado", gestor,
                "{\"estado\":\"EN_PROCESO\"}"));
        JsonNode eventosMantenimiento = eventos("mantenimiento", id);
        assertEquals(3, eventosMantenimiento.size());
        assertEquals(List.of("COMPLETAR_MANTENIMIENTO", "INICIAR_MANTENIMIENTO", "CREAR"),
                acciones(eventosMantenimiento));
        comprobarActor(eventosMantenimiento, gestorId, "rep_g_" + run);
        assertEquals("OPERATIVO", jdbc.queryForObject(
                "SELECT estado FROM equipo WHERE id_equipo = ?", String.class, equipoA1));
        comprobarSinSecretos(json(eventosMantenimiento), admin, gestor);
    }

    private int usuario(String etiqueta, String rol) {
        String username = "rep_" + etiqueta + "_" + run;
        int id = jdbc.queryForObject("""
                INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol)
                SELECT ?, 'Reporte', 'Fixture', ?, ?, id_rol FROM rol WHERE nombre = ?
                RETURNING id_usuario
                """, Integer.class, username, username + "@example.test", encoder.encode("Fixture-" + run), rol);
        usuariosPropios.add(id);
        return id;
    }

    private int sede(String etiqueta) {
        return jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede",
                Integer.class, "REP-" + etiqueta + "-" + run);
    }

    private int area(int idSede, String etiqueta) {
        return jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area",
                Integer.class, "REP-" + etiqueta + "-" + run, idSede);
    }

    private int laboratorio(int idArea, String etiqueta) {
        return jdbc.queryForObject(
                "INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio",
                Integer.class, "REP-" + etiqueta, "REP-" + etiqueta + "-" + run, idArea);
    }

    private int equipo(int idLaboratorio, String etiqueta, String estado, String fecha) {
        return jdbc.queryForObject("""
                INSERT INTO equipo(codigo_interno,nombre,estado,id_subcategoria,id_laboratorio,fecha_creacion)
                VALUES (?,?,?,?,?,?::timestamptz) RETURNING id_equipo
                """, Integer.class, "REP-" + etiqueta + "-" + run, "Equipo " + etiqueta,
                estado, subcategoria, idLaboratorio, fecha + "T12:00:00Z");
    }

    private void movimiento(int idEquipo, int origen, int destino, String fecha) {
        jdbc.update("""
                INSERT INTO movimiento_equipo(id_equipo,id_laboratorio_origen,id_laboratorio_destino,
                    id_usuario_actor,tipo_movimiento,motivo,fecha_movimiento)
                VALUES (?,?,?,?,'TRASLADO','Fixture de reportes',?::timestamptz)
                """, idEquipo, origen, destino, adminId, fecha + "T12:00:00Z");
    }

    private void mantenimiento(int idEquipo, String tipo, String estado, String fecha) {
        boolean completo = estado.equals("COMPLETADO");
        boolean cancelado = estado.equals("CANCELADO");
        jdbc.update("""
                INSERT INTO mantenimiento(id_equipo,tipo,descripcion,fecha_programada,estado,
                    fecha_inicio,fecha_fin,estado_equipo_anterior,fecha_creacion)
                VALUES (?,?,'Fixture de reportes',?,?,?::timestamptz,?::timestamptz,?,'2026-08-01T12:00:00Z')
                """, idEquipo, tipo, Date.valueOf(fecha), estado,
                completo ? fecha + "T08:00:00Z" : null,
                completo || cancelado ? fecha + "T09:00:00Z" : null,
                completo ? "OPERATIVO" : null);
    }

    private String tokenDe(String userName) {
        return jwtService.generateToken((UserInfoDetails) usuarios.loadUserByUsername(userName));
    }

    private String json(Object valor) { return mapper.writeValueAsString(valor); }

    private JsonNode get(String path, String bearer) throws Exception {
        return correcto(200, enviar("GET", path, bearer, null));
    }

    private JsonNode eventos(String entidad, int id) throws Exception {
        return get("/api/admin/auditoria?entidad=" + entidad + "&idEntidad=" + id, admin);
    }

    private HttpResponse<String> enviar(String method, String path, String bearer, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
        if (bearer != null) request.header("Authorization", "Bearer " + bearer);
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode correcto(int esperado, HttpResponse<String> response) {
        assertEquals(esperado, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    private long totalValor(JsonNode conteos, String valor) {
        for (JsonNode conteo : conteos) if (valor.equals(conteo.path("valor").asString()))
            return conteo.path("total").asLong();
        return 0;
    }

    private List<String> acciones(JsonNode eventos) {
        List<String> resultado = new ArrayList<>();
        for (JsonNode evento : eventos) resultado.add(evento.path("accion").asString());
        return resultado;
    }

    private void comprobarActor(JsonNode eventos, int id, String username) {
        assertFalse(eventos.isEmpty());
        for (JsonNode evento : eventos) {
            assertEquals(id, evento.path("idUsuarioActor").asInt());
            assertEquals(username, evento.path("userNameActor").asString());
            assertFalse(evento.path("fecha").asString().isBlank());
            assertFalse(evento.path("cambios").asString().isBlank());
        }
    }

    private void comprobarSinSecretos(String contenido, String... secretos) {
        for (String secreto : secretos) assertFalse(contenido.contains(secreto), "No debe exponer un secreto");
        for (String nombre : List.of("passwordHash", "password_hash", "accessToken", "JWT_SECRET", "DB_PASSWORD"))
            assertFalse(contenido.contains(nombre), "No debe exponer datos privados");
    }

    private void assertError(int esperado, HttpResponse<String> response) {
        JsonNode error = correcto(esperado, response);
        assertEquals(esperado, error.path("status").asInt());
        assertFalse(error.has("trace"));
        for (String privado : List.of("org.hibernate", "password_hash", "org.postgresql", "SELECT ", "INSERT "))
            assertFalse(response.body().contains(privado), "No debe exponer detalles internos");
    }
}
