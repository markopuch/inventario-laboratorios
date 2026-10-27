package com.utec.inventario.security;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(45)
class LoginRateLimitIntegrationTests {
    @TestConfiguration(proxyBeanMethods = false)
    static class VerificationConfiguration {
        @Bean AtomicLong loginVerificationClock() { return new AtomicLong(); }
        @Bean Set<String> loginVerificationPeers() { return ConcurrentHashMap.newKeySet(); }
        @Bean @Primary LoginRateLimiter verificationLimiter(AtomicLong loginVerificationClock,
                @Qualifier("loginVerificationPeers") Set<String> peers) {
            return new LoginRateLimiter(5, 900, 60, 60, 1000, loginVerificationClock::get) {
                @Override public synchronized void checkSource(String remoteAddress) {
                    peers.add(remoteAddress);
                    super.checkSource(remoteAddress);
                }
            };
        }
    }

    @Autowired private AtomicLong loginVerificationClock;
    @Autowired @Qualifier("loginVerificationPeers") private Set<String> peers;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarios;
    @LocalServerPort private int port;
    private HttpClient client;

    @BeforeEach void prepare() {
        loginVerificationClock.addAndGet(Duration.ofHours(1).toNanos());
        peers.clear();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }
    @AfterEach void close() { client.close(); }

    @Test
    void sextoIntentoBloqueadoAunqueLaClaveSeaCorrectaYElNombreCambieMayusculas() throws Exception {
        for (int i = 0; i < 5; i++) assertEquals(401, login("  MaRkO  ", wrong()).statusCode());
        HttpResponse<String> blocked = login("marko", demo());
        assertRateLimit(blocked, 900);
        assertFalse(blocked.body().contains("marko"));
        assertFalse(blocked.body().contains("accessToken"));
    }

    @Test
    void exitoDesdeOtroPeerReiniciaLosFallosAgregadosDelNombre() throws Exception {
        for (int i = 0; i < 4; i++) assertEquals(401, loginFromPeer("marko", wrong(), i).statusCode());
        assertEquals(200, loginFromPeer("marko", demo(), 4).statusCode());
        for (int i = 0; i < 5; i++) assertEquals(401, loginFromPeer("marko", wrong(), i).statusCode());
        assertRateLimit(loginFromPeer("marko", demo(), 5), 900);
        assertPeers(6);
    }

    @Test
    void vuelveAPermitirLoginAlExpirarLaVentanaSinDormirNiModificarCuentas() throws Exception {
        for (int i = 0; i < 5; i++) assertEquals(401, login("marko", wrong()).statusCode());
        assertRateLimit(login("marko", demo()), 900);
        loginVerificationClock.addAndGet(Duration.ofSeconds(900).toNanos());
        assertEquals(200, login("marko", demo()).statusCode());
    }

    @Test
    void usuarioExistenteYDesconocidoMantienenElMismoErrorYElMismoLimite() throws Exception {
        String missing = "ausente-" + UUID.randomUUID().toString().substring(0, 12);
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> existing = login("marko", wrong());
            HttpResponse<String> unknown = login(missing, wrong());
            assertEquals(401, existing.statusCode()); assertEquals(401, unknown.statusCode());
            assertEquals(mapper.readTree(existing.body()).path("message"), mapper.readTree(unknown.body()).path("message"));
        }
        HttpResponse<String> existing = login("marko", demo());
        HttpResponse<String> unknown = login(missing, wrong());
        assertRateLimit(existing, 900); assertRateLimit(unknown, 900);
        assertEquals(mapper.readTree(existing.body()).path("message"), mapper.readTree(unknown.body()).path("message"));
    }

    @Test
    void origenLimitaTambienJsonInvalidoIgnoraHeadersFalsificadosYNoAfectaOtrasRutas() throws Exception {
        for (int i = 0; i < 60; i++) {
            assertEquals(400, request("POST", "/api/auth/login", "{}", null, "192.0.2." + (i + 1)).statusCode());
        }
        assertRateLimit(request("POST", "/api/auth/login", "{}", null, "198.51.100.1"), 60);
        assertEquals(200, request("GET", "/actuator/health", null, null, null).statusCode());
        String token = jwtService.generateToken(usuarios.loadUserByUsername("marko"));
        assertEquals(200, request("GET", "/api/auth/me", null, token, null).statusCode());
        assertEquals(401, request("GET", "/api/auth/me", null, null, null).statusCode());
    }

    @Test
    void nuevasConexionesDesdeCincoPeersNoEludenElSexto429() throws Exception {
        String missing = "rotacion-" + UUID.randomUUID().toString().substring(0, 12);
        for (int i = 0; i < 5; i++) {
            String user = i % 2 == 0 ? "  " + missing.toUpperCase(java.util.Locale.ROOT) + "  " : missing;
            assertEquals(401, loginFromPeer(user, wrong(), i).statusCode());
        }
        assertRateLimit(loginFromPeer(missing, wrong(), 5), 900);
        assertPeers(6);
    }

    @Test
    void nombresDistintosMantienenPresupuestosIndependientesAunqueCambienPeers() throws Exception {
        String first = "primero-" + UUID.randomUUID().toString().substring(0, 12);
        String second = "segundo-" + UUID.randomUUID().toString().substring(0, 12);
        for (int i = 0; i < 5; i++) assertEquals(401, loginFromPeer(first, wrong(), i).statusCode());
        assertRateLimit(loginFromPeer(first, wrong(), 5), 900);
        assertEquals(401, loginFromPeer(second, wrong(), 5).statusCode());
        assertPeers(6);
    }

    @Test
    void solicitudesConcurrentesConPeersRealesCompartenCincoReservas() throws Exception {
        String missing = "paralelo-" + UUID.randomUUID().toString().substring(0, 12);
        try (var executor = Executors.newFixedThreadPool(20)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                int peer = i;
                results.add(executor.submit(() -> {
                    start.await();
                    return loginFromPeer(missing, wrong(), peer).statusCode();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var result : results) statuses.add(result.get());
            assertEquals(5, statuses.stream().filter(status -> status == 401).count());
            assertEquals(15, statuses.stream().filter(status -> status == 429).count());
            assertRateLimit(loginFromPeer(missing, wrong(), 20), 900);
            assertPeers(21);
        }
    }

    private HttpResponse<String> login(String user, String password) throws Exception {
        return request("POST", "/api/auth/login", mapper.writeValueAsString(Map.of("userName", user, "password", password)), null, null);
    }
    private HttpResponse<String> loginFromPeer(String user, String password, int index) throws Exception {
        // Cambia la dirección TCP real, no un header que la aplicación deba confiar.
        try (HttpClient transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .localAddress(InetAddress.getByName("127.0.0." + (index + 2))).build()) {
            return request(transport, "POST", "/api/auth/login",
                    mapper.writeValueAsString(Map.of("userName", user, "password", password)), null, null);
        }
    }
    private void assertPeers(int count) {
        for (int i = 0; i < count; i++) assertTrue(peers.contains("127.0.0." + (i + 2)),
                "El servidor debe haber recibido cada peer TCP real de la prueba");
    }
    private HttpResponse<String> request(String method, String path, String body, String token, String spoofed) throws Exception {
        return request(client, method, path, body, token, spoofed);
    }
    private HttpResponse<String> request(HttpClient transport, String method, String path, String body, String token, String spoofed) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20)).method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) builder.header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (spoofed != null) builder.header("X-Forwarded-For", spoofed).header("Forwarded", "for=" + spoofed);
        return transport.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private void assertRateLimit(HttpResponse<String> response, int seconds) throws Exception {
        assertEquals(429, response.statusCode());
        assertEquals(Integer.toString(seconds), response.headers().firstValue("Retry-After").orElseThrow());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
        JsonNode error = mapper.readTree(response.body());
        assertEquals(429, error.path("status").asInt());
        assertEquals("Too Many Requests", error.path("error").asString());
        assertEquals("/api/auth/login", error.path("path").asString());
        assertEquals(com.utec.inventario.exception.LoginRateLimitException.MESSAGE, error.path("message").asString());
        assertFalse(error.has("password"));
    }
    private String demo() { return System.getenv("DEMO_USER_PASSWORD"); }
    private String wrong() { return UUID.randomUUID().toString(); }
}
