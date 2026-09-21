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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.security.JwtService;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.UsuarioService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(40)
class SubcategoriaConcurrenciaTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(25);
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @LocalServerPort private int port;
    private String token;

    @BeforeEach
    void autenticar() {
        token = jwtService.generateToken((UserInfoDetails) usuarioService.loadUserByUsername("marko"));
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void altaOMovimientoYBajaDelPadreSeSerializanEnAmbosOrdenes(boolean mover, boolean padrePrimero)
            throws Exception {
        Integer source = categoria();
        Integer destination = categoria();
        Integer child = mover ? hija(source) : null;
        String body = "{\"nombre\":\"Hija concurrente\",\"idCategoria\":" + destination + "}";
        HttpRequest write = solicitud("/api/subcategorias" + (mover ? "/" + child : ""))
                .method(mover ? "PUT" : "POST", HttpRequest.BodyPublishers.ofString(body)).build();
        HttpRequest delete = solicitud("/api/categorias/" + destination).DELETE().build();

        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "SELECT id_categoria FROM categoria WHERE id_categoria = ? FOR UPDATE", destination);
            CompletableFuture<HttpResponse<String>> first = client.sendAsync(padrePrimero ? delete : write,
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            CompletableFuture<HttpResponse<String>> second = client.sendAsync(padrePrimero ? write : delete,
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();

            HttpResponse<String> firstResponse = first.get(25, TimeUnit.SECONDS);
            HttpResponse<String> secondResponse = second.get(25, TimeUnit.SECONDS);
            assertEquals(padrePrimero ? 204 : (mover ? 200 : 201), firstResponse.statusCode(), firstResponse.body());
            assertEquals(409, secondResponse.statusCode(), secondResponse.body());
            assertEquals(0, jdbc.queryForObject("""
                    SELECT count(*) FROM subcategoria s JOIN categoria c USING(id_categoria)
                    WHERE s.activo AND NOT c.activo AND c.id_categoria IN (?, ?)
                    """, Integer.class, source, destination));
            assertEquals(!padrePrimero, jdbc.queryForObject(
                    "SELECT activo FROM categoria WHERE id_categoria = ?", Boolean.class, destination));
            assertEquals(padrePrimero ? 0 : 1, jdbc.queryForObject(
                    "SELECT count(*) FROM subcategoria WHERE id_categoria = ? AND activo", Integer.class, destination));
            if (mover && padrePrimero) {
                assertEquals(source, jdbc.queryForObject("SELECT id_categoria FROM subcategoria WHERE id_subcategoria = ?",
                        Integer.class, child));
            }
        } finally {
            limpiar(source, destination);
        }
    }

    @Test
    void putConcurrenteNoReactivaLaHijaDadaDeBaja() throws Exception {
        Integer parent = categoria();
        Integer child = hija(parent);
        String path = "/api/subcategorias/" + child;
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "SELECT id_subcategoria FROM subcategoria WHERE id_subcategoria = ? FOR UPDATE", child);
            CompletableFuture<HttpResponse<String>> delete = client.sendAsync(solicitud(path).DELETE().build(),
                    HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            CompletableFuture<HttpResponse<String>> put = client.sendAsync(solicitud(path).PUT(
                    HttpRequest.BodyPublishers.ofString("{\"nombre\":\"Reactivada\",\"idCategoria\":" + parent + "}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> deleted = delete.get(25, TimeUnit.SECONDS);
            HttpResponse<String> updated = put.get(25, TimeUnit.SECONDS);
            assertEquals(204, deleted.statusCode(), deleted.body());
            assertEquals(404, updated.statusCode(), updated.body());
            assertEquals(Boolean.FALSE, jdbc.queryForObject(
                    "SELECT activo FROM subcategoria WHERE id_subcategoria = ?", Boolean.class, child));
            assertEquals("Original", jdbc.queryForObject(
                    "SELECT nombre FROM subcategoria WHERE id_subcategoria = ?", String.class, child));
        } finally {
            limpiar(parent, parent);
        }
    }

    @Test
    void indiceResuelveDuplicadoConcurrenteConError409Seguro() throws Exception {
        Integer parent = categoria();
        Integer child = hija(parent);
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            // Cambiar solo nombre evita un bloqueo FK del padre que ocultaría la carrera del índice.
            try (PreparedStatement update = external.prepareStatement(
                    "UPDATE subcategoria SET nombre = 'Sensores' WHERE id_subcategoria = ?")) {
                update.setInt(1, child);
                assertEquals(1, update.executeUpdate());
            }
            CompletableFuture<HttpResponse<String>> post = client.sendAsync(solicitud("/api/subcategorias").POST(
                    HttpRequest.BodyPublishers.ofString("{\"nombre\":\"sensores\",\"idCategoria\":" + parent + "}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            external.commit();
            HttpResponse<String> response = post.get(25, TimeUnit.SECONDS);
            assertEquals(409, response.statusCode(), response.body());
            assertTrue(response.body().contains("subcategoría"), response.body());
            assertFalse(response.body().contains("uq_subcategoria"));
            assertFalse(response.body().contains("org.hibernate"));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM subcategoria WHERE id_categoria = ?",
                    Integer.class, parent));
        } finally {
            limpiar(parent, parent);
        }
    }

    private Integer categoria() {
        return jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria",
                Integer.class, "S4A concurrencia " + UUID.randomUUID());
    }

    private Integer hija(Integer parent) {
        return jdbc.queryForObject("INSERT INTO subcategoria(nombre, id_categoria) VALUES ('Original', ?) RETURNING id_subcategoria",
                Integer.class, parent);
    }

    private void limpiar(Integer a, Integer b) {
        jdbc.update("DELETE FROM subcategoria WHERE id_categoria IN (?, ?)", a, b);
        jdbc.update("DELETE FROM categoria WHERE id_categoria IN (?, ?)", a, b);
    }

    private void bloquear(Connection connection, String sql, int id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(25);
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) { assertTrue(rows.next()); }
        }
    }

    private HttpRequest.Builder solicitud(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    private void esperarBloqueos(int minimum) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        int blocked = 0;
        while (System.nanoTime() < deadline) {
            blocked = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND usename = current_user
                      AND state = 'active' AND wait_event_type = 'Lock'
                      AND query ILIKE '%categoria%' AND pid <> pg_backend_pid()
                    """, Integer.class);
            if (blocked >= minimum) return;
            Thread.sleep(25);
        }
        assertTrue(blocked >= minimum, "Se esperaban " + minimum + " solicitudes bloqueadas; hubo " + blocked);
    }
}
