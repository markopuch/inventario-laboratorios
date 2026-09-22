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
import java.util.LinkedHashMap;
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
import org.junit.jupiter.params.provider.CsvSource;
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
class EquipoConcurrenciaTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(25);
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private PasswordEncoder passwordEncoder;
    @LocalServerPort private int port;

    private final List<Integer> subcategorias = new ArrayList<>();
    private final List<Integer> laboratorios = new ArrayList<>();
    private Integer categoria;
    private Integer sede;
    private Integer gestor;
    private String adminToken;
    private String gestorToken;
    private String codigo;

    @BeforeEach
    void prepararFixturesPropios() {
        String run = UUID.randomUUID().toString().replace("-", "");
        codigo = "EQ-CONC-" + run;
        adminToken = jwtService.generateToken(usuarioService.loadUserByUsername("marko"));
        categoria = jdbc.queryForObject("INSERT INTO categoria(nombre) VALUES (?) RETURNING id_categoria",
                Integer.class, "Equipo concurrente " + run);
        sede = jdbc.queryForObject("INSERT INTO sede(nombre) VALUES (?) RETURNING id_sede",
                Integer.class, "Equipo concurrente " + run);
        Integer area = jdbc.queryForObject("INSERT INTO area(nombre,id_sede) VALUES (?,?) RETURNING id_area",
                Integer.class, "Equipo concurrente " + run, sede);
        for (int i = 0; i < 2; i++) {
            subcategorias.add(jdbc.queryForObject("""
                    INSERT INTO subcategoria(nombre,id_categoria) VALUES (?,?) RETURNING id_subcategoria
                    """, Integer.class, "Subcategoría " + i, categoria));
            laboratorios.add(jdbc.queryForObject("""
                    INSERT INTO laboratorio(nombre,codigo,id_area) VALUES (?,?,?) RETURNING id_laboratorio
                    """, Integer.class, "Laboratorio " + i, "S5C" + i + run.substring(0, 20), area));
        }
        String username = "s5c_" + run;
        gestor = jdbc.queryForObject("""
                INSERT INTO usuario(username,nombre,apellido,email,password_hash,id_rol)
                SELECT ?,?,?,?,?,id_rol FROM rol WHERE nombre='GESTOR' RETURNING id_usuario
                """, Integer.class, username, "Prueba", "Concurrente", username + "@inventario.test",
                passwordEncoder.encode(UUID.randomUUID().toString()));
        gestorToken = jwtService.generateToken(usuarioService.loadUserByUsername(username));
    }

    @AfterEach
    void limpiarSoloFixturesPropiosDeLaBaseTemporal() {
        if (categoria != null) {
            jdbc.update("DELETE FROM equipo WHERE id_subcategoria IN (SELECT id_subcategoria FROM subcategoria WHERE id_categoria=?)", categoria);
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
    @CsvSource({"subcategoria,false", "subcategoria,true", "laboratorio,false", "laboratorio,true"})
    void crearEquipoYBajaDePadreRespetanAmbosOrdenes(String tabla, boolean bajaPrimero) throws Exception {
        int parent = tabla.equals("subcategoria") ? subcategorias.getFirst() : laboratorios.getFirst();
        HttpRequest post = crear(adminToken, codigo, 0);
        HttpRequest delete = solicitud("/api/" + tabla + "s/" + parent, adminToken).DELETE().build();
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, tabla, parent);
            var uno = client.sendAsync(bajaPrimero ? delete : post, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(bajaPrimero ? post : delete, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(bajaPrimero ? 204 : 201, first.statusCode(), first.body());
            assertEquals(409, second.statusCode(), second.body());
            assertEquals(!bajaPrimero, jdbc.queryForObject("SELECT activo FROM " + tabla + " WHERE id_" + tabla + "=?",
                    Boolean.class, parent));
            assertEquals(bajaPrimero ? 0 : 1, contarCodigo());
            assertEquals(0, jdbc.queryForObject("""
                    SELECT count(*) FROM equipo e JOIN subcategoria s USING(id_subcategoria)
                    JOIN laboratorio l USING(id_laboratorio)
                    WHERE e.codigo_interno=? AND e.estado<>'BAJA' AND (NOT s.activo OR NOT l.activo)
                    """, Integer.class, codigo));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void putYDeleteSerializanElEquipoSinReactivarLaBaja(boolean bajaPrimero) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            HttpResponse<String> created = client.send(crear(adminToken, codigo, 0), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, created.statusCode(), created.body());
            int equipo = mapper.readTree(created.body()).path("id").asInt();
            Map<String, Object> original = jdbc.queryForMap(
                    "SELECT fecha_creacion,fecha_actualizacion FROM equipo WHERE id_equipo=?", equipo);
            Map<String, Object> changes = campos(codigo, 0);
            changes.remove("codigoInterno");
            changes.remove("idLaboratorio");
            changes.put("nombre", "Nombre editado");
            changes.put("estado", "INOPERATIVO");
            HttpRequest put = solicitud("/api/equipos/" + equipo, adminToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(changes))).build();
            HttpRequest delete = solicitud("/api/equipos/" + equipo, adminToken).DELETE().build();
            external.setAutoCommit(false);
            bloquear(external, "equipo", equipo);
            var uno = client.sendAsync(bajaPrimero ? delete : put, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(bajaPrimero ? put : delete, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(bajaPrimero ? 204 : 200, first.statusCode(), first.body());
            assertEquals(bajaPrimero ? 409 : 204, second.statusCode(), second.body());
            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM equipo WHERE id_equipo=?", equipo);
            assertEquals("BAJA", row.get("estado"));
            assertEquals(bajaPrimero ? "Equipo concurrente" : "Nombre editado", row.get("nombre"));
            assertEquals(original.get("fecha_creacion"), row.get("fecha_creacion"));
            assertNotEquals(original.get("fecha_actualizacion"), row.get("fecha_actualizacion"));
            assertEquals(codigo, row.get("codigo_interno"));
            assertEquals(laboratorios.getFirst(), row.get("id_laboratorio"));
            HttpResponse<String> detail = client.send(solicitud("/api/equipos/" + equipo, adminToken).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, detail.statusCode(), detail.body());
            assertEquals("BAJA", mapper.readTree(detail.body()).path("estado").asString());
        }
    }

    @Test
    void indiceUnicoResuelveUnaInsercionInvisibleParaLaPreconsulta() throws Exception {
        // Padres distintos evitan que su bloqueo serialice la prueba antes del INSERT.
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            try (PreparedStatement insert = external.prepareStatement("""
                    INSERT INTO equipo(codigo_interno,nombre,id_subcategoria,id_laboratorio) VALUES (?,?,?,?)
                    """)) {
                insert.setQueryTimeout(25);
                insert.setString(1, codigo);
                insert.setString(2, "Inserción concurrente sin confirmar");
                insert.setInt(3, subcategorias.get(1));
                insert.setInt(4, laboratorios.get(1));
                assertEquals(1, insert.executeUpdate());
            }
            var post = client.sendAsync(crear(adminToken, codigo, 0), HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            external.commit();
            HttpResponse<String> response = post.get(25, TimeUnit.SECONDS);
            assertEquals(409, response.statusCode(), response.body());
            assertFalse(response.body().contains("uq_equipo"));
            assertFalse(response.body().contains("org.hibernate"));
            assertEquals(1, contarCodigo());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void revocarAlcanceYCrearComoGestorSeCoordinanSinPermisosObsoletos(boolean revocarPrimero) throws Exception {
        jdbc.update("INSERT INTO usuario_laboratorio(id_usuario,id_laboratorio) VALUES (?,?)", gestor, laboratorios.getFirst());
        HttpRequest post = crear(gestorToken, codigo, 0);
        HttpRequest revoke = solicitud("/api/admin/usuarios/" + gestor + "/laboratorios", adminToken)
                .PUT(HttpRequest.BodyPublishers.ofString("{\"idsLaboratorio\":[]}")).build();
        try (HttpClient client = HttpClient.newHttpClient(); Connection external = dataSource.getConnection()) {
            external.setAutoCommit(false);
            bloquear(external, "usuario", gestor);
            var uno = client.sendAsync(revocarPrimero ? revoke : post, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(1);
            var dos = client.sendAsync(revocarPrimero ? post : revoke, HttpResponse.BodyHandlers.ofString());
            esperarBloqueos(2);
            external.commit();
            HttpResponse<String> first = uno.get(25, TimeUnit.SECONDS);
            HttpResponse<String> second = dos.get(25, TimeUnit.SECONDS);
            assertEquals(revocarPrimero ? 200 : 201, first.statusCode(), first.body());
            assertEquals(revocarPrimero ? 403 : 200, second.statusCode(), second.body());
            assertEquals(revocarPrimero ? 0 : 1, contarCodigo());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario_laboratorio WHERE id_usuario=? AND activo",
                    Integer.class, gestor));
        }
    }

    private int contarCodigo() {
        return jdbc.queryForObject("SELECT count(*) FROM equipo WHERE codigo_interno=?", Integer.class, codigo);
    }

    private Map<String, Object> campos(String code, int index) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("codigoInterno", code);
        fields.put("nombre", "Equipo concurrente");
        fields.put("estado", "OPERATIVO");
        fields.put("requiereMantenimiento", false);
        fields.put("idSubcategoria", subcategorias.get(index));
        fields.put("idLaboratorio", laboratorios.get(index));
        return fields;
    }

    private HttpRequest crear(String token, String code, int index) {
        return solicitud("/api/equipos", token)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(campos(code, index)))).build();
    }

    private HttpRequest.Builder solicitud(String path, String token) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    private void bloquear(Connection connection, String tabla, Integer id) throws Exception {
        // tabla solo procede de los literales privados/parametrizados de esta clase.
        try (PreparedStatement statement = connection.prepareStatement("SELECT id_" + tabla + " FROM " + tabla
                + " WHERE id_" + tabla + "=? FOR UPDATE")) {
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
                    AND (query ILIKE '%equipo%' OR query ILIKE '%subcategoria%' OR query ILIKE '%laboratorio%'
                         OR query ILIKE '%usuario%') AND pid<>pg_backend_pid()
                    """, Integer.class);
            if (blocked >= minimo) return;
            Thread.sleep(25);
        }
        assertTrue(blocked >= minimo, "Las solicitudes no alcanzaron el bloqueo coordinado.");
    }
}
