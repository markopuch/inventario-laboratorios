package com.utec.inventario.config;

import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.utec.inventario.controller.AuthController;
import com.utec.inventario.exception.ApiError;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI inventarioOpenApi() {
        Components components = new Components().addSecuritySchemes(BEARER_AUTH,
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Obtener accessToken mediante POST /api/auth/login. En Authorize pegar solo el token, sin el prefijo Bearer."))
                .addResponses("NotFound", respuestaError("Recurso inexistente o no disponible según las reglas del módulo."))
                .addResponses("Conflict", respuestaError("Conflicto de unicidad, dependencias o estado de negocio."));
        ModelConverters.getInstance().read(ApiError.class).forEach(components::addSchemas);
        return new OpenAPI()
                .info(new Info().title("API Inventario de Laboratorios").version("0.0.1-SNAPSHOT")
                        .description("Contratos vigentes V1–V13. Roles ADMIN, GESTOR y LECTOR; el servidor valida "
                                + "usuario activo y alcance por laboratorio en cada petición. ADMIN tiene alcance global; "
                                + "GESTOR y LECTOR requieren asignaciones activas. Equipos y Auditoría admiten page/size "
                                + "opcionales (base cero, tamaño por defecto 20, máximo 100); sin ambos conservan arreglos. "
                                + "Los filtros de instantes usan días UTC; la programación de mantenimiento usa fechas de calendario. "
                                + "Los CSV se generan en el frontend a partir de los reportes agregados. No existen refresh token, "
                                + "logout con revocación ni registro público. Try it out ejecuta solicitudes reales en el servidor seleccionado."))
                .servers(List.of(new Server().url("/").description("Mismo servidor que sirve esta documentación")))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OperationCustomizer erroresDocumentados() {
        return (operation, handler) -> {
            boolean login = handler.getBeanType() == AuthController.class && handler.getMethod().getName().equals("login");
            agregarError(operation, "400", "JSON, parámetros o campos inválidos.");
            agregarError(operation, "401", login ? "Credenciales inválidas o usuario/rol inactivo."
                    : "JWT ausente, inválido o vencido; usuario/rol inactivo.");
            if (!login) {
                agregarError(operation, "403", "Rol o alcance de laboratorios insuficiente.");
            }
            agregarError(operation, "500", "Error interno; la respuesta no expone SQL ni trazas.");
            return operation;
        };
    }

    private static void agregarError(io.swagger.v3.oas.models.Operation operation, String status, String description) {
        if (!operation.getResponses().containsKey(status)) {
            operation.getResponses().addApiResponse(status, respuestaError(description));
        }
    }

    private static ApiResponse respuestaError(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiError"))));
    }
}
