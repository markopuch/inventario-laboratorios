package com.utec.inventario.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException exception, WebRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación.", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException exception, WebRequest request) {
        // La restricción de PostgreSQL cubre también dos escrituras concurrentes.
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && esRestriccionDeNombre(violation.getConstraintName())) {
                return error(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre.", request);
            }
            // Los metadatos del driver no dependen del idioma del mensaje de PostgreSQL.
            if (cause instanceof PSQLException postgresException
                    && "23505".equals(postgresException.getSQLState())
                    && postgresException.getServerErrorMessage() != null
                    && esRestriccionDeNombre(postgresException.getServerErrorMessage().getConstraint())) {
                return error(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre.", request);
            }
        }
        return handleUnexpected(exception, request);
    }

    private boolean esRestriccionDeNombre(@Nullable String nombre) {
        return "uq_categoria_nombre".equals(nombre)
                || "uq_categoria_nombre_ignore_case".equals(nombre);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Error interno al procesar la solicitud", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Inténtalo más tarde.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage() == null
                    ? "Valor inválido" : fieldError.getDefaultMessage();
            errors.putIfAbsent(fieldError.getField(), message);
        }

        return new ResponseEntity<>(body(status, "La solicitud contiene campos inválidos.", request, errors),
                headers, status);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception exception, @Nullable Object originalBody, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = switch (status.value()) {
            case 400 -> "La solicitud no es válida. Revisa el JSON y los parámetros enviados.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "El método HTTP no está permitido para este recurso.";
            case 415 -> "El tipo de contenido no está admitido. Usa application/json.";
            default -> status.is5xxServerError()
                    ? "Ocurrió un error interno. Inténtalo más tarde."
                    : "No se pudo procesar la solicitud.";
        };
        if (status.is5xxServerError()) {
            log.error("Error interno al procesar la solicitud", exception);
        }
        return super.handleExceptionInternal(exception, body(status, message, request, Map.of()),
                headers, status, request);
    }

    private ResponseEntity<Object> error(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(body(status, message, request, Map.of()));
    }

    private ApiError body(HttpStatusCode status, String message, WebRequest request, Map<String, String> errors) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        String reason = httpStatus == null ? "Error" : httpStatus.getReasonPhrase();
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        return ApiError.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(reason)
                .message(message)
                .path(path)
                .errors(errors)
                .build();
    }
}
