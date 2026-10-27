package com.utec.inventario.security;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
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
        @Bean @Primary LoginRateLimiter verificationLimiter(AtomicLong loginVerificationClock) {
            return new LoginRateLimiter(3, 30, 10, 60, 1000, loginVerificationClock::get);
        }
    }

    @Autowired private AtomicLong loginVerificationClock;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarios;
    @LocalServerPort private int port;
    private HttpClient client;

    @BeforeEach void prepare() {
        loginVerificationClock.addAndGet(Duration.ofHours(1).toNanos());
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }
    @AfterEach void close() { client.close(); }

    @Test
    void cuartoIntentoBloqueadoAunqueLaClaveSeaCorrectaYElNombreCambieMayusculas() throws Exception {
        for (int i = 0; i < 3; i++) assertEquals(401, login("  MaRkO  ", wrong()).statusCode());
        HttpResponse<String> blocked = login("marko", demo());
        assertRateLimit(blocked, 30);
        assertFalse(blocked.body().contains("marko"));
        assertFalse(blocked.body().contains("accessToken"));
    }

    @Test
    void exitoReiniciaFallosSinEliminarElLimiteGlobalDelOrigen() throws Exception {
        for (int i = 0; i < 2; i++) assertEquals(401, login("marko", wrong()).statusCode());
        assertEquals(200, login("marko", demo()).statusCode());
        for (int i = 0; i < 3; i++) assertEquals(401, login("marko", wrong()).statusCode());
        assertRateLimit(login("marko", demo()), 30);
    }

    @Test
    void vuelveAPermitirLoginAlExpirarLaVentanaSinDormirNiModificarCuentas() throws Exception {
        for (int i = 0; i < 3; i++) assertEquals(401, login("marko", wrong()).statusCode());
        assertRateLimit(login("marko", demo()), 30);
        loginVerificationClock.addAndGet(Duration.ofSeconds(30).toNanos());
        assertEquals(200, login("marko", demo()).statusCode());
    }

    @Test
    void usuarioExistenteYDesconocidoMantienenElMismoErrorYElMismoLimite() throws Exception {
        String missing = "ausente-" + UUID.randomUUID().toString().substring(0, 12);
        for (int i = 0; i < 3; i++) {
            HttpResponse<String> existing = login("marko", wrong());
            HttpResponse<String> unknown = login(missing, wrong());
            assertEquals(401, existing.statusCode()); assertEquals(401, unknown.statusCode());
            assertEquals(mapper.readTree(existing.body()).path("message"), mapper.readTree(unknown.body()).path("message"));
        }
        HttpResponse<String> existing = login("marko", demo());
        HttpResponse<String> unknown = login(missing, wrong());
        assertRateLimit(existing, 30); assertRateLimit(unknown, 30);
        assertEquals(mapper.readTree(existing.body()).path("message"), mapper.readTree(unknown.body()).path("message"));
    }

    @Test
    void origenLimitaTambienJsonInvalidoIgnoraHeadersFalsificadosYNoAfectaOtrasRutas() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(400, request("POST", "/api/auth/login", "{}", null, "192.0.2." + (i + 1)).statusCode());
        }
        assertRateLimit(request("POST", "/api/auth/login", "{}", null, "198.51.100.1"), 60);
        assertEquals(200, request("GET", "/actuator/health", null, null, null).statusCode());
        String token = jwtService.generateToken(usuarios.loadUserByUsername("marko"));
        assertEquals(200, request("GET", "/api/auth/me", null, token, null).statusCode());
        assertEquals(401, request("GET", "/api/auth/me", null, null, null).statusCode());
    }

    private HttpResponse<String> login(String user, String password) throws Exception {
        return request("POST", "/api/auth/login", mapper.writeValueAsString(Map.of("userName", user, "password", password)), null, null);
    }
    private HttpResponse<String> request(String method, String path, String body, String token, String spoofed) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20)).method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) builder.header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (spoofed != null) builder.header("X-Forwarded-For", spoofed).header("Forwarded", "for=" + spoofed);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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
