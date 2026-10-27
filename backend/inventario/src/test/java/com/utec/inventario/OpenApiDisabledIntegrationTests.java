package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(30)
class OpenApiDisabledIntegrationTests {
    @LocalServerPort private int port;

    @Test
    void permiteDeshabilitarLaDocumentacionSinAbrirLaApi() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String path : new String[] {"/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
                    "/swagger-ui.html", "/swagger-ui/index.html"}) {
                assertEquals(404, client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode(), path);
            }
            assertEquals(401, client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/me"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
}
