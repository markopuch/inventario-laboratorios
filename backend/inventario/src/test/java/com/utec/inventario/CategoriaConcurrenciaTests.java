package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

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
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.UsuarioService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
class CategoriaConcurrenciaTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioService usuarioService;

    private String adminToken;

    @LocalServerPort
    private int port;

    @BeforeEach
    void autenticarAdministrador() {
        UserInfoDetails administrador = (UserInfoDetails) this.usuarioService.loadUserByUsername("marko");
        this.adminToken = this.jwtService.generateToken(administrador);
    }

    @Test
    @Timeout(30)
    void putConcurrenteNoReactivaUnaCategoriaQueSeEstaDandoDeBaja() throws Exception {
        String nombre = "Concurrencia baja " + UUID.randomUUID();
        Integer id = this.jdbc.queryForObject("""
                INSERT INTO categoria (nombre, descripcion)
                VALUES (?, ?) RETURNING id_categoria
                """, Integer.class, nombre, "Descripción original");

        // La conexión externa ordena las solicitudes sin modificar el esquema.
        // Se cierra antes que el cliente HTTP: libera el bloqueo incluso si falla una aserción.
        try (HttpClient client = nuevoCliente();
                Connection transaction = this.dataSource.getConnection()) {
            transaction.setAutoCommit(false);
            try (PreparedStatement statement = transaction.prepareStatement(
                    "SELECT id_categoria FROM categoria WHERE id_categoria = ? FOR UPDATE")) {
                statement.setQueryTimeout(30);
                statement.setInt(1, id);
                try (ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "Debe existir la fila usada por esta prueba");
                }
            }

            CompletableFuture<HttpResponse<String>> delete = client.sendAsync(
                    solicitud("/" + id).DELETE().build(), HttpResponse.BodyHandlers.ofString());
            esperarSolicitudesBloqueadas(1);

            CompletableFuture<HttpResponse<String>> put = client.sendAsync(
                    solicitud("/" + id).PUT(HttpRequest.BodyPublishers.ofString("""
                            {"nombre":"%s editada","descripcion":"Cambio concurrente"}
                            """.formatted(nombre))).build(), HttpResponse.BodyHandlers.ofString());
            esperarSolicitudesBloqueadas(2);

            transaction.commit();

            HttpResponse<String> deleteResponse = delete.get(30, TimeUnit.SECONDS);
            HttpResponse<String> putResponse = put.get(30, TimeUnit.SECONDS);
            assertEquals(204, deleteResponse.statusCode(), deleteResponse.body());
            assertEquals(404, putResponse.statusCode(), putResponse.body());
            assertEquals(Boolean.FALSE, this.jdbc.queryForObject(
                    "SELECT activo FROM categoria WHERE id_categoria = ?", Boolean.class, id));
            assertEquals("Descripción original", this.jdbc.queryForObject(
                    "SELECT descripcion FROM categoria WHERE id_categoria = ?", String.class, id));
            assertEquals(404, client.send(solicitud("/" + id).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally {
            // La limpieza pertenece exclusivamente a esta fixture UUID en la base temporal.
            this.jdbc.update("DELETE FROM categoria WHERE id_categoria = ?", id);
        }
    }

    @Test
    @Timeout(30)
    void altaDuplicadaConcurrenteDevuelve409CuandoPostgresqlDetectaElConflicto() throws Exception {
        String nombre = "Concurrencia Duplicada " + UUID.randomUUID();

        try (HttpClient client = nuevoCliente();
                Connection transaction = this.dataSource.getConnection()) {
            transaction.setAutoCommit(false);
            try (PreparedStatement statement = transaction.prepareStatement(
                    "INSERT INTO categoria (nombre, descripcion) VALUES (?, ?)")) {
                statement.setQueryTimeout(30);
                statement.setString(1, nombre);
                statement.setString(2, "Primera inserción aún sin confirmar");
                assertEquals(1, statement.executeUpdate());
            }

            // existsByNombreIgnoreCase no puede ver la fila sin COMMIT; el INSERT
            // debe esperar al índice único. Así ejercitamos el error real de PostgreSQL.
            CompletableFuture<HttpResponse<String>> post = client.sendAsync(
                    solicitud("").POST(HttpRequest.BodyPublishers.ofString("""
                            {"nombre":"%s","descripcion":"Segunda inserción"}
                            """.formatted(nombre.toLowerCase(Locale.ROOT)))).build(),
                    HttpResponse.BodyHandlers.ofString());
            esperarSolicitudesBloqueadas(1);

            transaction.commit();

            HttpResponse<String> response = post.get(30, TimeUnit.SECONDS);
            assertEquals(409, response.statusCode(), response.body());
            assertTrue(response.body().contains("Ya existe una categoría con ese nombre."), response.body());
            assertFalse(response.body().contains("org.hibernate"), response.body());
            assertFalse(response.body().contains("uq_categoria_nombre"), response.body());
            assertEquals(1, this.jdbc.queryForObject("""
                    SELECT COUNT(*) FROM categoria WHERE UPPER(nombre) = UPPER(?)
                    """, Integer.class, nombre).intValue());
        } finally {
            this.jdbc.update("DELETE FROM categoria WHERE UPPER(nombre) = UPPER(?)", nombre);
        }
    }

    private HttpClient nuevoCliente() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    private HttpRequest.Builder solicitud(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/api/categorias" + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + this.adminToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private void esperarSolicitudesBloqueadas(int minimo) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        int blocked = 0;
        while (System.nanoTime() < deadline) {
            blocked = this.jdbc.queryForObject("""
                    SELECT COUNT(*) FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND usename = current_user
                      AND state = 'active'
                      AND wait_event_type = 'Lock'
                      AND query ILIKE '%categoria%'
                      AND pid <> pg_backend_pid()
                    """, Integer.class);
            if (blocked >= minimo) {
                return;
            }
            Thread.sleep(25);
        }
        assertTrue(blocked >= minimo,
                "Se esperaban " + minimo + " solicitudes bloqueadas; se observaron " + blocked);
    }
}
