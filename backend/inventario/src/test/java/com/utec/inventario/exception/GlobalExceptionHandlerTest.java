package com.utec.inventario.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ServletWebRequest request = new ServletWebRequest(
            new MockHttpServletRequest("POST", "/api/categorias"));

    @ParameterizedTest
    @ValueSource(strings = { "uq_categoria_nombre", "uq_categoria_nombre_ignore_case" })
    void violacionDeNombreUnicoDevuelveConflictoSinDetallesSQL(String restriccion) {
        SQLException sqlException = new SQLException("Detalle SQL privado", "23505");
        ConstraintViolationException cause = new ConstraintViolationException(
                "insert into categoria (nombre) values ('dato interno')", sqlException, restriccion);
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Fallo de persistencia", new RuntimeException("Causa intermedia", cause));

        ResponseEntity<Object> respuesta = this.handler.handleIntegrity(exception, this.request);

        ApiError error = assertInstanceOf(ApiError.class, respuesta.getBody());
        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode()),
                () -> assertEquals(409, error.getStatus()),
                () -> assertEquals("Conflict", error.getError()),
                () -> assertEquals("Ya existe una categoría con ese nombre.", error.getMessage()),
                () -> assertEquals("/api/categorias", error.getPath()),
                () -> assertNotNull(error.getTimestamp()),
                () -> assertTrue(error.getErrors().isEmpty()),
                () -> assertFalse(error.toString().contains("Detalle SQL privado")),
                () -> assertFalse(error.toString().contains("dato interno")),
                () -> assertFalse(error.toString().contains(restriccion)));
    }

    @Test
    void duplicadoConMensajePostgresqlEnEspanolUsaRestriccionEstructurada() {
        // Formato del protocolo PostgreSQL: cada campo termina en NUL.
        ServerErrorMessage mensajeServidor = new ServerErrorMessage(
                "SERROR\0VERROR\0C23505\0"
                        + "Mllave duplicada viola restricción de unicidad\0"
                        + "DYa existe la llave (upper(nombre))=(DATO PRIVADO).\0"
                        + "nuq_categoria_nombre_ignore_case\0\0");
        PSQLException sqlException = new PSQLException(mensajeServidor);
        ConstraintViolationException cause = new ConstraintViolationException(
                "Fallo de persistencia privado", sqlException, (String) null);
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Fallo de persistencia", cause);
        assertNull(cause.getConstraintName());
        assertEquals("uq_categoria_nombre_ignore_case", mensajeServidor.getConstraint());

        ResponseEntity<Object> respuesta = this.handler.handleIntegrity(exception, this.request);

        ApiError error = assertInstanceOf(ApiError.class, respuesta.getBody());
        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode()),
                () -> assertEquals(409, error.getStatus()),
                () -> assertEquals("Ya existe una categoría con ese nombre.", error.getMessage()),
                () -> assertEquals("/api/categorias", error.getPath()),
                () -> assertFalse(error.toString().contains("DATO PRIVADO")),
                () -> assertFalse(error.toString().contains("uq_categoria_nombre_ignore_case")),
                () -> assertFalse(error.toString().contains("23505")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "uq_subcategoria_nombre_categoria", "uq_subcategoria_categoria_nombre_ignore_case" })
    void duplicadoDeSubcategoriaDevuelveConflictoPorCategoriaSinDetallesSQL(String restriccion) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "insert into subcategoria values ('DATO PRIVADO')",
                new SQLException("Detalle SQL privado", "23505"), restriccion);
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Fallo de persistencia", new RuntimeException("Causa intermedia", cause));
        ServletWebRequest solicitud = new ServletWebRequest(
                new MockHttpServletRequest("POST", "/api/subcategorias"));

        ResponseEntity<Object> respuesta = this.handler.handleIntegrity(exception, solicitud);

        ApiError error = assertInstanceOf(ApiError.class, respuesta.getBody());
        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode()),
                () -> assertEquals(409, error.getStatus()),
                () -> assertEquals("Ya existe una subcategoría con ese nombre en la categoría seleccionada.",
                        error.getMessage()),
                () -> assertEquals("/api/subcategorias", error.getPath()),
                () -> assertNotNull(error.getTimestamp()),
                () -> assertTrue(error.getErrors().isEmpty()),
                () -> assertFalse(error.toString().contains("Detalle SQL privado")),
                () -> assertFalse(error.toString().contains("DATO PRIVADO")),
                () -> assertFalse(error.toString().contains(restriccion)));
    }

    @Test
    void duplicadoDeSubcategoriaEnEspanolReconoceIndiceDesdeMetadatosPostgresql() {
        ServerErrorMessage mensajeServidor = new ServerErrorMessage(
                "SERROR\0VERROR\0C23505\0"
                        + "Mllave duplicada viola restricción de unicidad\0"
                        + "DYa existe la llave (id_categoria, upper(nombre))=(3, DATO PRIVADO).\0"
                        + "nuq_subcategoria_categoria_nombre_ignore_case\0\0");
        ConstraintViolationException cause = new ConstraintViolationException(
                "Fallo SQL privado", new PSQLException(mensajeServidor), (String) null);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Fallo de persistencia", cause);
        ServletWebRequest solicitud = new ServletWebRequest(
                new MockHttpServletRequest("PUT", "/api/subcategorias/7"));

        ResponseEntity<Object> respuesta = this.handler.handleIntegrity(exception, solicitud);

        ApiError error = assertInstanceOf(ApiError.class, respuesta.getBody());
        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode()),
                () -> assertEquals(409, error.getStatus()),
                () -> assertEquals("Ya existe una subcategoría con ese nombre en la categoría seleccionada.",
                        error.getMessage()),
                () -> assertEquals("/api/subcategorias/7", error.getPath()),
                () -> assertFalse(error.toString().contains("DATO PRIVADO")),
                () -> assertFalse(error.toString().contains("uq_subcategoria_categoria_nombre_ignore_case")),
                () -> assertFalse(error.toString().contains("23505")));
    }

    @Test
    void restriccionesDeOrganizacionDevuelven409PorMetadatosHibernateYPostgresql() {
        String[][] constraints = {
            { "uq_area_nombre_sede", "Ya existe un área con ese nombre en la sede seleccionada." },
            { "uq_area_sede_nombre_ignore_case", "Ya existe un área con ese nombre en la sede seleccionada." },
            { "uq_laboratorio_codigo", "Ya existe un laboratorio con ese código." },
            { "uq_laboratorio_codigo_ignore_case", "Ya existe un laboratorio con ese código." }
        };
        for (String[] constraint : constraints) {
            for (boolean hibernateIdentificaConstraint : new boolean[] { true, false }) {
                ServerErrorMessage metadata = new ServerErrorMessage(
                        "SERROR\0VERROR\0C23505\0Mllave duplicada viola restricción de unicidad\0"
                                + "DDetalle privado de persistencia\0n" + constraint[0] + "\0\0");
                ConstraintViolationException cause = new ConstraintViolationException(
                        "SQL privado", new PSQLException(metadata),
                        hibernateIdentificaConstraint ? constraint[0] : null);
                ResponseEntity<Object> response = handler.handleIntegrity(
                        new DataIntegrityViolationException("Fallo interno", cause), request);
                ApiError error = assertInstanceOf(ApiError.class, response.getBody());
                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                assertEquals(constraint[1], error.getMessage());
                assertFalse(error.toString().contains(constraint[0]));
                assertFalse(error.toString().contains("privado"));
                assertFalse(error.toString().contains("23505"));
            }
        }
    }

    @Test
    void otraRestriccionDevuelveErrorInternoGenericoSinExponerLaCausa() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "insert into tabla_privada values ('dato interno')",
                new SQLException("Detalle SQL privado", "23503"), "fk_desconocida");
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Fallo de persistencia privado", cause);

        ResponseEntity<Object> respuesta = this.handler.handleIntegrity(exception, this.request);

        ApiError error = assertInstanceOf(ApiError.class, respuesta.getBody());
        assertAll(
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode()),
                () -> assertEquals(500, error.getStatus()),
                () -> assertEquals("Internal Server Error", error.getError()),
                () -> assertEquals("Ocurrió un error interno. Inténtalo más tarde.", error.getMessage()),
                () -> assertEquals("/api/categorias", error.getPath()),
                () -> assertNotNull(error.getTimestamp()),
                () -> assertTrue(error.getErrors().isEmpty()),
                () -> assertFalse(error.toString().contains("privad")),
                () -> assertFalse(error.toString().contains("dato interno")),
                () -> assertFalse(error.toString().contains("fk_desconocida")));
    }
}
