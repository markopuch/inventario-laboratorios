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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(90)
class UsuarioLaboratorioConcurrenciaTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private PasswordEncoder passwordEncoder;
    @LocalServerPort private int port;

    private final List<Integer> usuarios = new ArrayList<>();
    private final List<Integer> laboratorios = new ArrayList<>();
    private Integer sede;
    private String token;

    @BeforeEach
    void prepararFixturesPropios() {
        token = jwtService.generateToken(usuarioService.loadUserByUsername("marko"));
        String run = UUID.randomUUID().toString().replace("-", "");
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede",
                Integer.class, "Concurrencia alcance " + run);
        Integer area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area",
                Integer.class, "Área alcance " + run, sede);
        for (int i = 0; i < 3; i++) {
            laboratorios.add(jdbc.queryForObject("""
                    INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio
                    """, Integer.class, "Laboratorio alcance " + i, "SC" + i + run.substring(0, 20), area));
        }
        for (int i = 0; i < 2; i++) {
            usuarios.add(jdbc.queryForObject("""
                    INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol)
                    SELECT ?,?,?,?,?,id_rol FROM rol WHERE nombre='GESTOR' RETURNING id_usuario
                    """, Integer.class, "scope_" + i + run, "Prueba", "Alcance",
                    "scope_" + i + run + "@inventario.test", passwordEncoder.encode(UUID.randomUUID().toString())));
        }
    }

    @AfterEach
    void limpiarSoloFixturesDeLaBaseTemporal() {
        for (Integer usuario : usuarios) {
            jdbc.update("DELETE FROM usuario_laboratorio WHERE id_usuario = ?", usuario);
            jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", usuario);
        }
        if (sede != null) {
            jdbc.update("DELETE FROM laboratorio WHERE id_area IN (SELECT id_area FROM area WHERE id_sede = ?)", sede);
            jdbc.update("DELETE FROM area WHERE id_sede = ?", sede);
            jdbc.update("DELETE FROM sede WHERE id_sede = ?", sede);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void asignacionYBajaDeLaboratorioRespetanAmbosOrdenes(boolean bajaPrimero) throws Exception {
        Integer usuario = usuarios.getFirst();
        Integer laboratorio = laboratorios.getFirst();
        HttpRequest asignar = reemplazo(usuario, List.of(laboratorio));
        HttpRequest baja = solicitud("/api/laboratorios/" + laboratorio).DELETE().build();

        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "laboratorio", laboratorio, false);
            var primera = client.sendAsync(bajaPrimero ? baja : asignar, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var segunda = client.sendAsync(bajaPrimero ? asignar : baja, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();

            HttpResponse<String> primeraRespuesta = primera.get(20, TimeUnit.SECONDS);
            HttpResponse<String> segundaRespuesta = segunda.get(20, TimeUnit.SECONDS);
            assertEquals(bajaPrimero ? 204 : 200, primeraRespuesta.statusCode(), primeraRespuesta.body());
            assertEquals(409, segundaRespuesta.statusCode(), segundaRespuesta.body());
            assertEquals(!bajaPrimero, jdbc.queryForObject(
                    "SELECT activo FROM laboratorio WHERE id_laboratorio = ?", Boolean.class, laboratorio));
            assertEquals(bajaPrimero ? 0 : 1, jdbc.queryForObject("""
                    SELECT count(*) FROM usuario_laboratorio WHERE id_usuario = ? AND activo
                    """, Integer.class, usuario));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT count(*) FROM usuario_laboratorio ul JOIN laboratorio l USING(id_laboratorio)
                    WHERE ul.id_usuario = ? AND ul.activo AND NOT l.activo
                    """, Integer.class, usuario));
        }
    }

    @Test
    void dosPutDelMismoUsuarioDejanElConjuntoCompletoDeLaUltimaOperacion() throws Exception {
        Integer usuario = usuarios.getFirst();
        List<Integer> primero = laboratorios.subList(0, 2);
        List<Integer> ultimo = List.of(laboratorios.get(2));

        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "usuario", usuario, false);
            var uno = client.sendAsync(reemplazo(usuario, primero), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(reemplazo(usuario, ultimo), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();

            HttpResponse<String> respuestaUno = uno.get(20, TimeUnit.SECONDS);
            HttpResponse<String> respuestaDos = dos.get(20, TimeUnit.SECONDS);
            assertEquals(200, respuestaUno.statusCode(), respuestaUno.body());
            assertEquals(200, respuestaDos.statusCode(), respuestaDos.body());
            assertEquals(primero, idsRespuesta(respuestaUno));
            assertEquals(ultimo, idsRespuesta(respuestaDos));
            assertEquals(ultimo, asignacionesActivas(usuario));
            assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM usuario_laboratorio WHERE id_usuario = ?",
                    Integer.class, usuario), "Las asignaciones anteriores se conservan inactivas.");
        }
    }

    @Test
    void usuariosDistintosOrdenanLosBloqueosAunqueRecibanIdsEnOrdenInverso() throws Exception {
        Integer menor = laboratorios.get(0);
        Integer mayor = laboratorios.get(1);
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "laboratorio", menor, false);
            var uno = client.sendAsync(reemplazo(usuarios.get(0), List.of(mayor, menor)),
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            // Si el PUT siguiera el orden del request, ya tendría bloqueado el ID mayor.
            // NOWAIT comprueba el orden real sin depender de tiempos arbitrarios.
            bloquear(external, "laboratorio", mayor, true);
            var dos = client.sendAsync(reemplazo(usuarios.get(1), List.of(menor, mayor)),
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();

            HttpResponse<String> respuestaUno = uno.get(20, TimeUnit.SECONDS);
            HttpResponse<String> respuestaDos = dos.get(20, TimeUnit.SECONDS);
            assertEquals(200, respuestaUno.statusCode(), respuestaUno.body());
            assertEquals(200, respuestaDos.statusCode(), respuestaDos.body());
            assertEquals(List.of(menor, mayor), asignacionesActivas(usuarios.get(0)));
            assertEquals(List.of(menor, mayor), asignacionesActivas(usuarios.get(1)));
        }
    }

    private List<Integer> idsRespuesta(HttpResponse<String> respuesta) {
        List<Integer> ids = new ArrayList<>();
        mapper.readTree(respuesta.body()).path("laboratorios").forEach(lab -> ids.add(lab.path("id").asInt()));
        return ids;
    }

    private List<Integer> asignacionesActivas(Integer usuario) {
        return jdbc.queryForList("""
                SELECT id_laboratorio FROM usuario_laboratorio WHERE id_usuario = ? AND activo ORDER BY id_laboratorio
                """, Integer.class, usuario);
    }

    private HttpRequest reemplazo(Integer usuario, List<Integer> ids) {
        return solicitud("/api/admin/usuarios/" + usuario + "/laboratorios").PUT(
                HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("idsLaboratorio", ids)))).build();
    }

    private HttpRequest.Builder solicitud(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    private void bloquear(Connection connection, String tabla, Integer id, boolean nowait) throws Exception {
        // tabla procede exclusivamente de los dos literales privados de esta clase.
        try (PreparedStatement statement = connection.prepareStatement("SELECT id_" + tabla + " FROM " + tabla
                + " WHERE id_" + tabla + " = ? FOR UPDATE" + (nowait ? " NOWAIT" : ""))) {
            statement.setQueryTimeout(20);
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) { assertTrue(rows.next()); }
        }
    }

    private void esperarBloqueos(int minimo) throws InterruptedException {
        long limite = System.nanoTime() + TIMEOUT.toNanos();
        int bloqueados = 0;
        while (System.nanoTime() < limite) {
            bloqueados = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND usename = current_user
                      AND state = 'active' AND wait_event_type = 'Lock'
                      AND (query ILIKE '%usuario%' OR query ILIKE '%laboratorio%')
                      AND pid <> pg_backend_pid()
                    """, Integer.class);
            if (bloqueados >= minimo) return;
            Thread.sleep(25);
        }
        assertTrue(bloqueados >= minimo, "No llegaron las solicitudes concurrentes esperadas al bloqueo.");
    }
}
