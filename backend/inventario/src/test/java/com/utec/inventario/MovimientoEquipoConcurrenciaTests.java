package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.MovimientoEquipoService;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(90)
class MovimientoEquipoConcurrenciaTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(25);
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MovimientoEquipoService movimientoService;
    @LocalServerPort private int port;

    private final List<Integer> laboratorios = new ArrayList<>();
    private Integer categoria;
    private Integer subcategoria;
    private Integer sede;
    private Integer gestor;
    private Integer equipo;
    private Integer admin;
    private String adminToken;
    private String gestorToken;
    private String run;

    @BeforeEach
    void prepararFixturesPropios() {
        run = UUID.randomUUID().toString().replace("-", "");
        UserInfoDetails principal = (UserInfoDetails) usuarioService.loadUserByUsername("marko");
        admin = principal.getId();
        adminToken = jwtService.generateToken(principal);
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria",
                Integer.class, "Movimiento concurrente " + run);
        subcategoria = jdbc.queryForObject("INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria",
                Integer.class, "Subcategoría " + run, categoria);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede",
                Integer.class, "Movimiento concurrente " + run);
        Integer area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area",
                Integer.class, "Área " + run, sede);
        for (int i = 0; i < 3; i++) {
            laboratorios.add(jdbc.queryForObject("""
                    INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio
                    """, Integer.class, "Laboratorio " + i, "S6C" + i + run.substring(0, 20), area));
        }
        String username = "s6c_" + run;
        gestor = jdbc.queryForObject("""
                INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol)
                SELECT ?,?,?,?,?,id_rol FROM rol WHERE nombre='GESTOR' RETURNING id_usuario
                """, Integer.class, username, "Prueba", "Concurrente", username + "@inventario.test",
                passwordEncoder.encode(UUID.randomUUID().toString()));
        gestorToken = jwtService.generateToken(usuarioService.loadUserByUsername(username));
        equipo = crearEquipoSql(laboratorios.get(0), "A");
    }

    @AfterEach
    void limpiarSoloFixturesDeLaBaseTemporal() {
        if (subcategoria != null) {
            jdbc.update("DELETE FROM movimiento_equipo WHERE id_equipo IN (SELECT id_equipo FROM equipo WHERE id_subcategoria=?)", subcategoria);
            jdbc.update("DELETE FROM equipo WHERE id_subcategoria=?", subcategoria);
        }
        if (gestor != null) {
            jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario=?", gestor);
            jdbc.update("DELETE FROM usuario WHERE id_usuario=?", gestor);
        }
        if (categoria != null) {
            jdbc.update("DELETE FROM subcategoria WHERE id_categoria=?", categoria);
            jdbc.update("DELETE FROM categoria WHERE id_categoria=?", categoria);
        }
        if (sede != null) {
            jdbc.update("DELETE FROM laboratorio WHERE id_area IN (SELECT id_area FROM area WHERE id_sede=?)", sede);
            jdbc.update("DELETE FROM area WHERE id_sede=?", sede);
            jdbc.update("DELETE FROM sede WHERE id_sede=?", sede);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void dosTrasladosSerializanOrigenRealYNoDuplicanElMismoDestino(boolean mismoDestino) throws Exception {
        int primero = laboratorios.get(1);
        int segundo = laboratorios.get(mismoDestino ? 1 : 2);
        Map<String, Object> original = filaEquipo(equipo);
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "equipo", equipo, false);
            var uno = client.sendAsync(traslado(equipo, primero, adminToken), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(traslado(equipo, segundo, adminToken), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(200, first.statusCode(), first.body());
            assertEquals(mismoDestino ? 409 : 200, second.statusCode(), second.body());
            List<Map<String, Object>> history = movimientos(equipo);
            assertEquals(mismoDestino ? 1 : 2, history.size());
            assertMovimiento(history.get(0), laboratorios.get(0), primero, admin);
            if (!mismoDestino) assertMovimiento(history.get(1), primero, segundo, admin);
            Map<String, Object> current = filaEquipo(equipo);
            assertEquals(segundo, current.get("id_laboratorio"));
            for (String field : List.of("id_equipo", "codigo_interno", "id_subcategoria", "id_responsable", "fecha_creacion")) {
                assertEquals(original.get(field), current.get(field), field);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trasladoYBajaDelEquipoConservanMovimientoYEstadoEnAmbosOrdenes(boolean bajaPrimero) throws Exception {
        HttpRequest transfer = traslado(equipo, laboratorios.get(1), adminToken);
        HttpRequest delete = solicitud("/api/equipos/" + equipo, adminToken).DELETE().build();
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "equipo", equipo, false);
            var uno = client.sendAsync(bajaPrimero ? delete : transfer, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(bajaPrimero ? transfer : delete, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(bajaPrimero ? 204 : 200, first.statusCode(), first.body());
            assertEquals(bajaPrimero ? 409 : 204, second.statusCode(), second.body());
            assertEquals("BAJA", filaEquipo(equipo).get("estado"));
            assertEquals(laboratorios.get(bajaPrimero ? 0 : 1), filaEquipo(equipo).get("id_laboratorio"));
            assertEquals(bajaPrimero ? 0 : 1, movimientos(equipo).size());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trasladoYRevocacionRespetanElAlcanceVigenteAlAdquirirElActor(boolean revocarPrimero) throws Exception {
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?),(?,?)",
                gestor, laboratorios.get(0), gestor, laboratorios.get(1));
        HttpRequest transfer = traslado(equipo, laboratorios.get(1), gestorToken);
        HttpRequest revoke = solicitud("/api/admin/usuarios/" + gestor + "/laboratorios", adminToken)
                .PUT(HttpRequest.BodyPublishers.ofString("{\"idsLaboratorio\":[]}")).build();
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "usuario", gestor, false);
            var uno = client.sendAsync(revocarPrimero ? revoke : transfer, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(revocarPrimero ? transfer : revoke, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(200, first.statusCode(), first.body());
            assertEquals(revocarPrimero ? 403 : 200, second.statusCode(), second.body());
            assertEquals(laboratorios.get(revocarPrimero ? 0 : 1), filaEquipo(equipo).get("id_laboratorio"));
            assertEquals(revocarPrimero ? 0 : 1, movimientos(equipo).size());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario_laboratorio WHERE id_usuario=? AND activo", Integer.class, gestor));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trasladoYBajaDelDestinoNuncaDejanEquipoVigenteEnLaboratorioInactivo(boolean bajaPrimero) throws Exception {
        int destino = laboratorios.get(1);
        HttpRequest transfer = traslado(equipo, destino, adminToken);
        HttpRequest delete = solicitud("/api/laboratorios/" + destino, adminToken).DELETE().build();
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "laboratorio", destino, false);
            var uno = client.sendAsync(bajaPrimero ? delete : transfer, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(bajaPrimero ? transfer : delete, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(bajaPrimero ? 204 : 200, first.statusCode(), first.body());
            assertEquals(409, second.statusCode(), second.body());
            assertEquals(bajaPrimero ? 0 : 1, movimientos(equipo).size());
            assertEquals(laboratorios.get(bajaPrimero ? 0 : 1), filaEquipo(equipo).get("id_laboratorio"));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT count(*) FROM equipo e JOIN laboratorio l USING(id_laboratorio)
                    WHERE e.id_equipo=? AND e.estado<>'BAJA' AND NOT l.activo
                    """, Integer.class, equipo));
        }
    }

    @Test
    void equiposDiferentesEnSentidosOpuestosBloqueanLosLaboratoriosPorIdAscendente() throws Exception {
        int menor = laboratorios.get(0);
        int mayor = laboratorios.get(1);
        int otro = crearEquipoSql(mayor, "B");
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "laboratorio", menor, false);
            var uno = client.sendAsync(traslado(otro, menor, adminToken), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            // Si el traslado bloqueara origen antes de destino, ya tendría tomado el mayor.
            bloquear(external, "laboratorio", mayor, true);
            var dos = client.sendAsync(traslado(equipo, mayor, adminToken), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(200, first.statusCode(), first.body());
            assertEquals(200, second.statusCode(), second.body());
            assertEquals(menor, filaEquipo(otro).get("id_laboratorio"));
            assertEquals(mayor, filaEquipo(equipo).get("id_laboratorio"));
            assertMovimiento(movimientos(otro).getFirst(), mayor, menor, admin);
            assertMovimiento(movimientos(equipo).getFirst(), menor, mayor, admin);
        }
    }

    @Test
    void falloRealDePersistenciaDelMovimientoRevierteElEquipoYaEnviadoAPostgresql() {
        Map<String, Object> original = filaEquipo(equipo);
        // Se omite intencionalmente el Controller/@Size para ejercitar VARCHAR(500) de V3.
        // No hay cambios de esquema ni puntos de fallo artificiales en producción.
        assertThrows(DataIntegrityViolationException.class, () -> movimientoService.trasladarEquipo(
                admin, equipo, laboratorios.get(1), "X".repeat(501), "Ubicación que debe revertirse"));
        assertEquals(original, filaEquipo(equipo));
        assertTrue(movimientos(equipo).isEmpty());
    }

    private int crearEquipoSql(int laboratorio, String sufijo) {
        return jdbc.queryForObject("""
                INSERT INTO equipo(codigo_interno,nombre,id_subcategoria,id_laboratorio,id_responsable,ubicacion_interna)
                VALUES (?,?,?,?,?,?) RETURNING id_equipo
                """, Integer.class, "EQ-S6C-" + run + sufijo, "Equipo concurrente", subcategoria,
                laboratorio, gestor, "Ubicación original");
    }

    private Map<String, Object> filaEquipo(int id) {
        return jdbc.queryForMap("SELECT * FROM equipo WHERE id_equipo=?", id);
    }

    private List<Map<String, Object>> movimientos(int id) {
        return jdbc.queryForList("""
                SELECT id_laboratorio_origen,id_laboratorio_destino,id_usuario_actor,tipo_movimiento
                FROM movimiento_equipo WHERE id_equipo=? ORDER BY id_movimiento
                """, id);
    }

    private void assertMovimiento(Map<String, Object> row, int origen, int destino, int actor) {
        assertEquals(origen, row.get("id_laboratorio_origen"));
        assertEquals(destino, row.get("id_laboratorio_destino"));
        assertEquals(actor, row.get("id_usuario_actor"));
        assertEquals("TRASLADO", row.get("tipo_movimiento"));
    }

    private HttpRequest traslado(int id, int destino, String token) {
        return solicitud("/api/equipos/" + id + "/traslados", token)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of(
                        "idLaboratorioDestino", destino, "motivo", "Movimiento concurrente",
                        "ubicacionInternaDestino", "Ubicación destino")))).build();
    }

    private HttpRequest.Builder solicitud(String path, String token) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    private void bloquear(Connection connection, String tabla, Integer id, boolean nowait) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id_" + tabla + " FROM " + tabla
                + " WHERE id_" + tabla + "=? FOR UPDATE" + (nowait ? " NOWAIT" : ""))) {
            statement.setQueryTimeout(25);
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) { assertTrue(rows.next()); }
        }
    }

    private void esperarBloqueos(int minimo) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        int blocked = 0;
        while (System.nanoTime() < deadline) {
            blocked = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity WHERE datname=current_database()
                    AND usename=current_user AND state='active' AND wait_event_type='Lock'
                    AND (query ILIKE '%equipo%' OR query ILIKE '%laboratorio%' OR query ILIKE '%usuario%')
                    AND pid<>pg_backend_pid()
                    """, Integer.class);
            if (blocked >= minimo) return;
            Thread.sleep(25);
        }
        assertTrue(blocked >= minimo, "Las solicitudes no alcanzaron el bloqueo coordinado.");
    }
}
