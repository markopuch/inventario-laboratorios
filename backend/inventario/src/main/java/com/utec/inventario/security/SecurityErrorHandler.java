package com.utec.inventario.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.utec.inventario.exception.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Autowired
    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        this.writeError(request, response, HttpStatus.UNAUTHORIZED,
                "Se requiere autenticación válida para acceder a este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        this.writeError(request, response, HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta operación.");
    }

    public void internalServerError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.writeError(request, response, HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Inténtalo más tarde.");
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String message) throws IOException {
        ApiError error = ApiError.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errors(Map.of())
                .build();

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        this.objectMapper.writeValue(response.getOutputStream(), error);
    }
}
