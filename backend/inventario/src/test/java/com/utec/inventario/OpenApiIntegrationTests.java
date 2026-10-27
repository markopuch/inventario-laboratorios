package com.utec.inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.utec.inventario.config.OpenApiConfig;
import com.utec.inventario.security.JwtService;
import com.utec.inventario.service.UsuarioService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL",
        matches = "jdbc:postgresql://[^/]+/inventario_verificacion_[a-zA-Z0-9_]+")
@Timeout(60)
class OpenApiIntegrationTests {

    private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");
    private static final Set<String> ALTAS = Set.of("/api/categorias", "/api/subcategorias", "/api/sedes", "/api/areas",
            "/api/laboratorios", "/api/equipos", "/api/admin/usuarios", "/api/mantenimientos");
    @Autowired private ObjectMapper mapper;
    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarios;
    @LocalServerPort private int port;
    private HttpClient client;

    @BeforeEach
    void abrirCliente() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    @AfterEach
    void cerrarCliente() {
        client.close();
    }

    @Test
    void publicaExactamenteLasSetentaOperacionesRegistradas() throws Exception {
        JsonNode api = especificacion();
        Set<String> reales = new HashSet<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (handler.getBeanType().getPackageName().equals("com.utec.inventario.controller")) {
                mapping.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods()
                        .forEach(method -> reales.add(method.name().toLowerCase() + " " + path)));
            }
        });
        assertEquals(70, reales.size());
        assertEquals(reales, operaciones(api));
        for (String key : reales) {
            String[] parts = key.split(" ", 2);
            JsonNode operation = api.path("paths").path(parts[1]).path(parts[0]);
            assertFalse(operation.path("summary").asString().isBlank(), key);
            assertFalse(operation.path("description").asString().isBlank(), key);
            assertTrue(operation.path("tags").size() > 0, key);
            assertTrue(operation.path("responses").has("401"), key);
            assertTrue(operation.path("responses").has("500"), key);
            String expectedStatus = parts[0].equals("delete") || parts[1].endsWith("/password") ? "204"
                    : parts[0].equals("post") && ALTAS.contains(parts[1]) ? "201" : "200";
            Set<String> success = new HashSet<>();
            operation.path("responses").properties().forEach(response -> {
                if (response.getKey().startsWith("2")) success.add(response.getKey());
            });
            assertEquals(Set.of(expectedStatus), success, key);
        }
        assertFalse(api.path("paths").has("/actuator/health"));
        verificarReferencias(api, api);
    }

    @Test
    void documentaJwtYLoginPublicoSinExponerElPrincipal() throws Exception {
        JsonNode api = especificacion();
        JsonNode scheme = api.path("components").path("securitySchemes").path(OpenApiConfig.BEARER_AUTH);
        assertEquals("http", scheme.path("type").asString());
        assertEquals("bearer", scheme.path("scheme").asString());
        assertEquals("JWT", scheme.path("bearerFormat").asString());
        assertTrue(api.path("security").get(0).has(OpenApiConfig.BEARER_AUTH));
        JsonNode login = api.path("paths").path("/api/auth/login").path("post");
        assertTrue(login.has("security"));
        assertEquals(0, login.path("security").size());
        assertFalse(api.toString().contains("UserInfoDetails"));
        assertFalse(api.toString().contains("passwordHash"));
        assertFalse(api.toString().contains("password_hash"));
        assertFalse(api.toString().contains("UsuarioEntity"));
        assertEquals("/", api.path("servers").get(0).path("url").asString());
    }

    @Test
    void documentaAltasBajasErroresYContrasenasSinCambiarContratos() throws Exception {
        JsonNode api = especificacion();
        for (String path : ALTAS) {
            JsonNode responses = api.path("paths").path(path).path("post").path("responses");
            assertTrue(responses.has("201"), path);
            assertFalse(responses.has("200"), path);
        }
        api.path("paths").properties().forEach(entry -> {
            if (entry.getValue().has("delete")) {
                JsonNode responses = entry.getValue().path("delete").path("responses");
                assertTrue(responses.has("204"), entry.getKey());
                assertFalse(responses.path("204").has("content"));
            }
        });
        JsonNode schemas = api.path("components").path("schemas");
        assertTrue(schemas.path("ApiError").path("properties").has("errors"));
        assertTrue(api.path("paths").path("/api/subcategorias").path("post").path("responses").has("404"));
        assertTrue(api.path("paths").path("/api/reportes/resumen").path("get").path("responses").has("404"));
        assertFalse(api.path("paths").path("/api/sedes").path("post").path("responses").has("409"));
        assertFalse(api.path("paths").path("/api/admin/usuarios/{id}/password").path("put").path("responses").has("409"));
        for (String name : Set.of("AuthRequest", "CreateUsuarioRequest", "CambiarPasswordUsuarioRequest")) {
            JsonNode password = schemas.path(name).path("properties").path("password");
            assertTrue(password.path("writeOnly").asBoolean(), name);
            assertEquals("password", password.path("format").asString(), name);
            assertFalse(schemas.path(name).path("properties").has("passwordSegura"));
            if (!name.equals("AuthRequest")) {
                assertEquals(15, password.path("minLength").asInt(), name);
                assertTrue(password.path("description").asString().contains("72 bytes"));
            }
        }
    }

    @Test
    void filtrosDeReportesSonParametrosQueryYNuncaUnBodyGet() throws Exception {
        JsonNode api = especificacion();
        for (String path : Set.of("/resumen", "/equipos/por-estado", "/equipos/por-laboratorio", "/movimientos", "/mantenimientos")) {
            JsonNode operation = api.path("paths").path("/api/reportes" + path).path("get");
            assertFalse(operation.has("requestBody"));
            Set<String> names = new HashSet<>();
            operation.path("parameters").forEach(parameter -> {
                assertEquals("query", parameter.path("in").asString());
                names.add(parameter.path("name").asString());
            });
            assertEquals(Set.of("idSede", "idArea", "idLaboratorio", "estado", "fechaDesde", "fechaHasta",
                    "estadoMantenimiento", "tipoMantenimiento"), names, path);
        }
    }

    @Test
    void postmanCubreLasMismasRutasYNoGuardaCredenciales() throws Exception {
        Path folder = Path.of("../../docs/backend-final/postman");
        JsonNode collection = mapper.readTree(Files.readString(folder.resolve("Inventario-Laboratorios.postman_collection.json")));
        Set<String> postman = new HashSet<>();
        recorrerPostman(collection.path("item"), postman);
        Set<String> expected = new HashSet<>();
        operaciones(especificacion()).forEach(operation -> expected.add(operation.replaceAll("\\{[^}]+}", "{}")));
        assertEquals(expected, postman);
        JsonNode environment = mapper.readTree(Files.readString(folder.resolve("Inventario-Laboratorios.postman_environment.json")));
        environment.path("values").forEach(variable -> {
            String key = variable.path("key").asString().toLowerCase();
            if (key.contains("password") || key.contains("token")) {
                assertEquals("", variable.path("value").asString(), key);
            }
        });
    }

    @Test
    void swaggerUiCargaSinPetstoreNiAutorizacionPersistente() throws Exception {
        HttpResponse<String> redirect = enviar("GET", "/swagger-ui.html", null);
        assertTrue(redirect.statusCode() == 302 || redirect.statusCode() == 307);
        assertTrue(redirect.headers().firstValue("location").orElseThrow().contains("/swagger-ui/index.html"));
        assertEquals(200, enviar("GET", "/swagger-ui/index.html", null).statusCode());
        assertEquals(200, enviar("GET", "/swagger-ui/swagger-ui-bundle.js", null).statusCode());
        JsonNode config = mapper.readTree(enviar("GET", "/v3/api-docs/swagger-config", null).body());
        assertEquals("/v3/api-docs", config.path("url").asString());
        assertFalse(config.path("persistAuthorization").asBoolean());
        assertFalse(config.toString().contains("petstore"));
        assertEquals(200, enviar("GET", "/v3/api-docs.yaml", null).statusCode());
    }

    @Test
    void documentacionPublicaNoAbreRutasDeNegocioNiMetodosDesconocidos() throws Exception {
        assertEquals(401, enviar("GET", "/api/equipos", null).statusCode());
        assertEquals(401, enviar("POST", "/api/categorias", null).statusCode());
        assertEquals(401, enviar("POST", "/v3/api-docs", null).statusCode());
        assertEquals(401, enviar("GET", "/v3/api-docs/no-existe", null).statusCode());
        String lector = jwtService.generateToken(usuarios.loadUserByUsername("romel"));
        assertEquals(403, enviar("GET", "/api/admin/auditoria", lector).statusCode());
        assertEquals(403, enviar("POST", "/api/categorias", lector).statusCode());
    }

    private JsonNode especificacion() throws Exception {
        HttpResponse<String> response = enviar("GET", "/v3/api-docs", null);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("json"));
        JsonNode api = mapper.readTree(response.body());
        assertTrue(api.path("openapi").asString().startsWith("3."));
        return api;
    }

    private Set<String> operaciones(JsonNode api) {
        Set<String> result = new HashSet<>();
        api.path("paths").properties().forEach(path -> path.getValue().properties().forEach(method -> {
            if (METHODS.contains(method.getKey())) result.add(method.getKey() + " " + path.getKey());
        }));
        return result;
    }

    private void recorrerPostman(JsonNode items, Set<String> result) {
        items.forEach(item -> {
            if (item.has("item")) recorrerPostman(item.path("item"), result);
            else {
                JsonNode request = item.path("request");
                String raw = request.path("url").isTextual() ? request.path("url").asString()
                        : request.path("url").path("raw").asString();
                String route = raw.replaceFirst("^\\{\\{baseUrl}}", "").split("\\?", 2)[0]
                        .replaceAll("\\{\\{[^}]+}}", "{}");
                result.add(request.path("method").asString().toLowerCase() + " " + route);
            }
        });
    }

    private void verificarReferencias(JsonNode node, JsonNode root) {
        if (node.isObject()) {
            node.properties().forEach(property -> {
                if (property.getKey().equals("$ref")) {
                    String ref = property.getValue().asString();
                    assertTrue(ref.startsWith("#/"), ref);
                    assertFalse(root.at(ref.substring(1)).isMissingNode(), ref);
                } else verificarReferencias(property.getValue(), root);
            });
        } else if (node.isArray()) node.forEach(child -> verificarReferencias(child, root));
    }

    private HttpResponse<String> enviar(String method, String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(45)).method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
