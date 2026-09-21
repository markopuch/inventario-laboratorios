package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.entity.CategoriaEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.repository.CategoriaRepository;
import com.utec.inventario.repository.SubcategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final OffsetDateTime FECHA_CREACION = OffsetDateTime.parse("2026-01-10T15:00:00Z");

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private SubcategoriaRepository subcategoriaRepository;

    private CategoriaService categoriaService;

    @BeforeEach
    void prepararServicio() {
        this.categoriaService = new CategoriaService(this.categoriaRepository,
                Mappers.getMapper(CategoriaMapper.class), this.subcategoriaRepository);
    }

    @Test
    void crearNormalizaDatosYDejaIdentidadYFechaALaPersistencia() {
        Categoria entrada = Categoria.builder()
                .id(999)
                .nombre("  Robótica  ")
                .descripcion("  Sensores  ")
                .activo(false)
                .fechaCreacion(FECHA_CREACION.minusDays(1))
                .build();
        when(this.categoriaRepository.existsByNombreIgnoreCase("Robótica")).thenReturn(false);
        when(this.categoriaRepository.saveAndFlush(any(CategoriaEntity.class))).thenAnswer(invocation -> {
            CategoriaEntity entity = invocation.getArgument(0);
            assertAll(
                    () -> assertNull(entity.getIdCategoria()),
                    () -> assertNull(entity.getFechaCreacion()),
                    () -> assertTrue(entity.isActivo()),
                    () -> assertEquals("Robótica", entity.getNombre()),
                    () -> assertEquals("Sensores", entity.getDescripcion()));
            // Simula únicamente los valores que la base de datos devuelve al guardar.
            entity.setIdCategoria(7);
            entity.setFechaCreacion(FECHA_CREACION);
            return entity;
        });

        Categoria resultado = this.categoriaService.crearCategoria(entrada);

        assertAll(
                () -> assertEquals(7, resultado.getId()),
                () -> assertEquals(FECHA_CREACION, resultado.getFechaCreacion()),
                () -> assertTrue(resultado.isActivo()));
        verify(this.categoriaRepository).existsByNombreIgnoreCase("Robótica");
        verify(this.categoriaRepository).saveAndFlush(any(CategoriaEntity.class));
    }

    @Test
    void crearRechazaNombreReservadoInclusoCuandoLaCategoriaEstaInactiva() {
        Categoria entrada = Categoria.builder().nombre("  robótica  ").build();
        // La consulta global no filtra por activo: una baja sigue reservando su nombre.
        when(this.categoriaRepository.existsByNombreIgnoreCase("robótica")).thenReturn(true);

        assertThrows(ConflictException.class, () -> this.categoriaService.crearCategoria(entrada));

        verify(this.categoriaRepository).existsByNombreIgnoreCase("robótica");
        verifyNoMoreInteractions(this.categoriaRepository);
    }

    @Test
    void actualizarPermiteNombrePropioYDescripcionNulaConservandoCamposDelServidor() {
        CategoriaEntity existente = categoriaActiva();
        Categoria cambios = Categoria.builder().nombre("  robótica  ").descripcion(null).build();
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(7)).thenReturn(Optional.of(existente));
        when(this.categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot("robótica", 7))
                .thenReturn(false);
        when(this.categoriaRepository.saveAndFlush(existente)).thenReturn(existente);

        Categoria resultado = this.categoriaService.actualizarCategoria(7, cambios);

        assertAll(
                () -> assertEquals(7, resultado.getId()),
                () -> assertEquals("robótica", resultado.getNombre()),
                () -> assertNull(resultado.getDescripcion()),
                () -> assertTrue(resultado.isActivo()),
                () -> assertEquals(FECHA_CREACION, resultado.getFechaCreacion()));
        verify(this.categoriaRepository).existsByNombreIgnoreCaseAndIdCategoriaNot("robótica", 7);
        verify(this.categoriaRepository).findForUpdateByIdCategoriaAndActivoTrue(7);
        verify(this.categoriaRepository).saveAndFlush(existente);
    }

    @Test
    void actualizarRechazaNombreDeOtraCategoriaAntesDeModificarLaEntidad() {
        CategoriaEntity existente = categoriaActiva();
        Categoria cambios = Categoria.builder().nombre("  Instrumentación  ").descripcion(null).build();
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(7)).thenReturn(Optional.of(existente));
        when(this.categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot("Instrumentación", 7))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> this.categoriaService.actualizarCategoria(7, cambios));

        assertAll(
                () -> assertEquals("Robótica", existente.getNombre()),
                () -> assertEquals("Sensores", existente.getDescripcion()),
                () -> assertTrue(existente.isActivo()));
        verify(this.categoriaRepository, never()).saveAndFlush(any(CategoriaEntity.class));
    }

    @Test
    void eliminarSoloMarcaInactivaLaMismaEntidadSinBorrarlaFisicamente() {
        CategoriaEntity existente = categoriaActiva();
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(7)).thenReturn(Optional.of(existente));
        when(this.subcategoriaRepository.existsByCategoria_IdCategoriaAndActivoTrue(7)).thenReturn(false);
        when(this.categoriaRepository.saveAndFlush(existente)).thenAnswer(invocation -> {
            assertSame(existente, invocation.getArgument(0));
            assertFalse(existente.isActivo());
            return existente;
        });

        this.categoriaService.eliminarCategoria(7);

        assertAll(
                () -> assertFalse(existente.isActivo()),
                () -> assertEquals(7, existente.getIdCategoria()),
                () -> assertEquals("Robótica", existente.getNombre()),
                () -> assertEquals(FECHA_CREACION, existente.getFechaCreacion()));
        verify(this.categoriaRepository).findForUpdateByIdCategoriaAndActivoTrue(7);
        verify(this.subcategoriaRepository).existsByCategoria_IdCategoriaAndActivoTrue(7);
        verify(this.categoriaRepository).saveAndFlush(existente);
        verifyNoMoreInteractions(this.categoriaRepository);
    }

    @Test
    void eliminarCategoriaConSubcategoriasActivasRechazaLaBajaSinModificarElPadre() {
        CategoriaEntity existente = categoriaActiva();
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(7)).thenReturn(Optional.of(existente));
        when(this.subcategoriaRepository.existsByCategoria_IdCategoriaAndActivoTrue(7)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> this.categoriaService.eliminarCategoria(7));

        assertAll(
                () -> assertTrue(existente.isActivo()),
                () -> assertEquals("No se puede desactivar la categoría porque contiene subcategorías activas.",
                        exception.getMessage()));
        verify(this.subcategoriaRepository).existsByCategoria_IdCategoriaAndActivoTrue(7);
        verify(this.categoriaRepository, never()).saveAndFlush(any(CategoriaEntity.class));
    }

    @Test
    void categoriaAusenteOInactivaNoSePuedeConsultarActualizarNiEliminar() {
        when(this.categoriaRepository.findByIdCategoriaAndActivoTrue(7)).thenReturn(Optional.empty());
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(7)).thenReturn(Optional.empty());
        Categoria cambios = Categoria.builder().nombre("Robótica").build();

        assertAll(
                () -> assertThrows(ResourceNotFoundException.class,
                        () -> this.categoriaService.obtenerCategoria(7)),
                () -> assertThrows(ResourceNotFoundException.class,
                        () -> this.categoriaService.actualizarCategoria(7, cambios)),
                () -> assertThrows(ResourceNotFoundException.class,
                        () -> this.categoriaService.eliminarCategoria(7)));

        verify(this.categoriaRepository).findByIdCategoriaAndActivoTrue(7);
        verify(this.categoriaRepository, times(2)).findForUpdateByIdCategoriaAndActivoTrue(7);
        verifyNoMoreInteractions(this.categoriaRepository);
    }

    private CategoriaEntity categoriaActiva() {
        return CategoriaEntity.builder()
                .idCategoria(7)
                .nombre("Robótica")
                .descripcion("Sensores")
                .activo(true)
                .fechaCreacion(FECHA_CREACION)
                .build();
    }
}
