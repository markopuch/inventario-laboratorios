package com.utec.inventario.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PaginacionRequestTest {
    private boolean valido(PaginacionRequest request) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(request).isEmpty();
        }
    }

    @Test
    void omitirAmbosConservaModoAnteriorYDefineValoresEfectivos() {
        PaginacionRequest request = new PaginacionRequest();
        assertFalse(request.isSolicitada());
        assertEquals(0, request.paginaEfectiva());
        assertEquals(20, request.tamanoEfectivo());
        assertTrue(valido(request));
    }

    @Test
    void cualquieraDeLosDosActivaPaginacionSinExigirElOtro() {
        PaginacionRequest soloPagina = new PaginacionRequest();
        soloPagina.setPage(3);
        assertTrue(soloPagina.isSolicitada());
        assertEquals(20, soloPagina.tamanoEfectivo());
        assertTrue(valido(soloPagina));
        PaginacionRequest soloTamano = new PaginacionRequest();
        soloTamano.setSize(100);
        assertTrue(soloTamano.isSolicitada());
        assertEquals(0, soloTamano.paginaEfectiva());
        assertTrue(valido(soloTamano));
    }

    @Test
    void rechazaLimitesInvalidosYDesplazamientosNoRepresentablesPorJpa() {
        PaginacionRequest request = new PaginacionRequest();
        request.setPage(-1);
        assertFalse(valido(request));
        request.setPage(0);
        for (int size : new int[] {0, -1, 101}) {
            request.setSize(size);
            assertFalse(valido(request));
        }
        request.setSize(100);
        request.setPage(Integer.MAX_VALUE);
        assertFalse(valido(request));
        request.setSize(1);
        assertTrue(valido(request));
    }

    @Test
    void unParametroPresenteVacioNoSeConvierteEnModoAnterior() {
        PaginacionRequest request = new PaginacionRequest();
        assertThrows(IllegalArgumentException.class, () -> request.setPage(null));
        assertThrows(IllegalArgumentException.class, () -> request.setSize(null));
    }
}
