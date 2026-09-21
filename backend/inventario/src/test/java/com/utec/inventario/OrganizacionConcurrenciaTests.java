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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(60)
class OrganizacionConcurrenciaTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ObjectMapper mapper;
    @LocalServerPort private int port;
    private String token;

    @BeforeEach
    void autenticarAdministrador() {
        token = jwtService.generateToken((UserInfoDetails) usuarioService.loadUserByUsername("marko"));
    }

    @ParameterizedTest
    @CsvSource({ "area,false", "area,true", "laboratorio,false", "laboratorio,true" })
    void altaOMovimientoYBajaDelPadreRespetanAmbosOrdenes(String tipo, boolean mover) throws Exception {
        // Cada caso compara ambos resultados posibles; no depende de temporizaciones arbitrarias.
        comprobarPadreHijo(tipo, mover, false);
        comprobarPadreHijo(tipo, mover, true);
    }

    private void comprobarPadreHijo(String tipo, boolean mover, boolean padrePrimero) throws Exception {
        Integer sedeA = sede();
        Integer sedeB = sede();
        boolean esArea = tipo.equals("area");
        String tablaPadre = esArea ? "sede" : "area";
        Integer origen = esArea ? sedeA : area(sedeA);
        Integer destino = esArea ? sedeB : area(sedeB);
        Integer hija = mover ? (esArea ? area(origen) : laboratorio(origen)) : null;
        String pathHija = "/api/" + tipo + "s" + (mover ? "/" + hija : "");
        Map<String, Object> datos = esArea
                ? Map.of("nombre", "Área concurrente", "idSede", destino)
                : Map.of("nombre", "Laboratorio concurrente", "codigo", codigo(), "idArea", destino);
        HttpRequest escritura = solicitud(pathHija).method(mover ? "PUT" : "POST",
                HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(datos))).build();
        HttpRequest baja = solicitud("/api/" + tablaPadre + "s/" + destino).DELETE().build();

        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, tablaPadre, destino);
            CompletableFuture<HttpResponse<String>> first = client.sendAsync(padrePrimero ? baja : escritura,
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            CompletableFuture<HttpResponse<String>> second = client.sendAsync(padrePrimero ? escritura : baja,
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();

            HttpResponse<String> firstResponse = first.get(20, TimeUnit.SECONDS);
            HttpResponse<String> secondResponse = second.get(20, TimeUnit.SECONDS);
            assertEquals(padrePrimero ? 204 : (mover ? 200 : 201), firstResponse.statusCode(), firstResponse.body());
            assertEquals(409, secondResponse.statusCode(), secondResponse.body());
            assertEquals(!padrePrimero, jdbc.queryForObject(
                    "SELECT activo FROM " + tablaPadre + " WHERE id_" + tablaPadre + " = ?", Boolean.class, destino));
            assertEquals(padrePrimero ? 0 : 1, jdbc.queryForObject(
                    "SELECT count(*) FROM " + tipo + " WHERE id_" + tablaPadre + " = ? AND activo", Integer.class, destino));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT count(*) FROM " + tipo + " h JOIN " + tablaPadre + " p USING(id_" + tablaPadre + ")"
                            + " WHERE h.activo AND NOT p.activo AND p.id_" + tablaPadre + " IN (?, ?)",
                    Integer.class, origen, destino));
            if (mover && padrePrimero) {
                assertEquals(origen, jdbc.queryForObject("SELECT id_" + tablaPadre + " FROM " + tipo
                        + " WHERE id_" + tipo + " = ?", Integer.class, hija));
            }
        } finally {
            limpiar(sedeA, sedeB);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "sede", "area", "laboratorio" })
    void putNoReactivaUnaEntidadQueDeleteAcabaDeDesactivar(String tipo) throws Exception {
        Integer sede = sede();
        Integer parent = sede;
        Integer id = sede;
        Map<String, Object> datos = Map.of("nombre", "Intento de cambio concurrente");
        if (tipo.equals("area")) {
            id = area(sede);
            datos = Map.of("nombre", "Intento de cambio concurrente", "idSede", sede);
        } else if (tipo.equals("laboratorio")) {
            parent = area(sede);
            id = laboratorio(parent);
            datos = Map.of("nombre", "Intento de cambio concurrente", "codigo", codigo(), "idArea", parent);
        }
        String nombreOriginal = jdbc.queryForObject("SELECT nombre FROM " + tipo + " WHERE id_" + tipo + " = ?",
                String.class, id);
        String path = "/api/" + tipo + "s/" + id;
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, tipo, id);
            CompletableFuture<HttpResponse<String>> delete = client.sendAsync(solicitud(path).DELETE().build(),
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            CompletableFuture<HttpResponse<String>> put = client.sendAsync(solicitud(path).PUT(
                    HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(datos))).build(),
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();

            HttpResponse<String> deleted = delete.get(20, TimeUnit.SECONDS);
            HttpResponse<String> updated = put.get(20, TimeUnit.SECONDS);
            assertEquals(204, deleted.statusCode(), deleted.body());
            assertEquals(404, updated.statusCode(), updated.body());
            assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM " + tipo
                    + " WHERE id_" + tipo + " = ?", Boolean.class, id));
            assertEquals(nombreOriginal, jdbc.queryForObject("SELECT nombre FROM " + tipo
                    + " WHERE id_" + tipo + " = ?", String.class, id));
        } finally {
            limpiar(sede, sede);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "area", "laboratorio" })
    void indiceUnicoResuelveCarreraDeDuplicadosCon409Seguro(String tipo) throws Exception {
        Integer sede = sede();
        boolean esArea = tipo.equals("area");
        Integer parent = esArea ? sede : area(sede);
        Integer existente = esArea ? area(sede) : laboratorio(parent);
        // Laboratorio se intenta crear en otra Área para comprobar unicidad GLOBAL.
        Integer destino = esArea ? sede : area(sede);
        String campo = esArea ? "nombre" : "codigo";
        String valor = esArea ? "Duplicado " + UUID.randomUUID() : codigo();
        Map<String, Object> datos = esArea
                ? Map.of("nombre", valor.toLowerCase(Locale.ROOT), "idSede", destino)
                : Map.of("nombre", "Duplicado global", "codigo", valor.toLowerCase(Locale.ROOT), "idArea", destino);

        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            // Cambia solo nombre/código sin tocar la FK: no bloquea el padre durante el precheck del Service.
            try (PreparedStatement update = external.prepareStatement(
                    "UPDATE " + tipo + " SET " + campo + " = ? WHERE id_" + tipo + " = ?")) {
                update.setString(1, valor);
                update.setInt(2, existente);
                assertEquals(1, update.executeUpdate());
            }
            CompletableFuture<HttpResponse<String>> post = client.sendAsync(solicitud("/api/" + tipo + "s")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(datos))).build(),
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            external.commit();

            HttpResponse<String> response = post.get(20, TimeUnit.SECONDS);
            assertEquals(409, response.statusCode(), response.body());
            assertEquals(409, mapper.readTree(response.body()).path("status").asInt());
            assertFalse(mapper.readTree(response.body()).path("message").asString().isBlank());
            assertFalse(response.body().contains("uq_"));
            assertFalse(response.body().contains("org.hibernate"));
            assertFalse(response.body().contains("SQL"));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM " + tipo + " WHERE UPPER(" + campo
                    + ") = UPPER(?)", Integer.class, valor));
        } finally {
            limpiar(sede, sede);
        }
    }

    private Integer sede() {
        return jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede",
                Integer.class, "Concurrencia sede " + UUID.randomUUID());
    }

    private Integer area(Integer sede) {
        return jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area",
                Integer.class, "Concurrencia área " + UUID.randomUUID(), sede);
    }

    private Integer laboratorio(Integer area) {
        return jdbc.queryForObject("INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio",
                Integer.class, "Concurrencia laboratorio", codigo(), area);
    }

    private String codigo() { return "ORG-" + UUID.randomUUID().toString().substring(0, 20).toUpperCase(Locale.ROOT); }

    private void limpiar(Integer sedeA, Integer sedeB) {
        jdbc.update("DELETE FROM laboratorio WHERE id_area IN (SELECT id_area FROM area WHERE id_sede IN (?, ?))",
                sedeA, sedeB);
        jdbc.update("DELETE FROM area WHERE id_sede IN (?, ?)", sedeA, sedeB);
        jdbc.update("DELETE FROM sede WHERE id_sede IN (?, ?)", sedeA, sedeB);
    }

    private void bloquear(Connection connection, String tabla, Integer id) throws Exception {
        // tabla solo procede de los literales definidos en estos tests, nunca de una petición.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id_" + tabla + " FROM " + tabla + " WHERE id_" + tabla + " = ? FOR UPDATE")) {
            statement.setQueryTimeout(20);
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) { assertTrue(rows.next()); }
        }
    }

    private HttpRequest.Builder solicitud(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    private void esperarBloqueos(int minimo) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        int blocked = 0;
        while (System.nanoTime() < deadline) {
            blocked = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND usename = current_user
                      AND state = 'active' AND wait_event_type = 'Lock'
                      AND (query ILIKE '%sede%' OR query ILIKE '%area%' OR query ILIKE '%laboratorio%')
                      AND pid <> pg_backend_pid()
                    """, Integer.class);
            if (blocked >= minimo) return;
            Thread.sleep(25);
        }
        assertTrue(blocked >= minimo, "Se esperaban " + minimo + " solicitudes bloqueadas; hubo " + blocked);
    }
}
