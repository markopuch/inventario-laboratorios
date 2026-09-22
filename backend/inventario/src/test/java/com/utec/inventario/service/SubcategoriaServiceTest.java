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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.entity.CategoriaEntity;
import com.utec.inventario.entity.SubcategoriaEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.SubcategoriaMapper;
import com.utec.inventario.repository.CategoriaRepository;
import com.utec.inventario.repository.SubcategoriaRepository;
import com.utec.inventario.repository.EquipoRepository;

@ExtendWith(MockitoExtension.class)
class SubcategoriaServiceTest {

    private static final OffsetDateTime FECHA_CREACION = OffsetDateTime.parse("2026-01-10T15:00:00Z");

    @Mock
    private SubcategoriaRepository subcategoriaRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private EquipoRepository equipoRepository;

    private SubcategoriaService service;

    @BeforeEach
    void prepararServicio() {
        this.service = new SubcategoriaService(this.subcategoriaRepository,
                this.categoriaRepository, Mappers.getMapper(SubcategoriaMapper.class), this.equipoRepository);
    }

    @Test
    void crearNormalizaDatosIgnoraCamposDelServidorYUsaLaEntidadPadreValidada() {
        CategoriaEntity padre = categoriaActiva(3);
        Subcategoria entrada = entrada(3, "  Sensores  ");
        entrada.setId(999);
        entrada.setDescripcion("  Medición  ");
        entrada.setActivo(false);
        entrada.setFechaCreacion(FECHA_CREACION.minusDays(1));
        entrada.getCategoria().setNombre("Nombre de padre no confiable");
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(3)).thenReturn(Optional.of(padre));
        when(this.subcategoriaRepository.saveAndFlush(any(SubcategoriaEntity.class))).thenAnswer(invocation -> {
            SubcategoriaEntity entity = invocation.getArgument(0);
            assertAll(
                    () -> assertNull(entity.getIdSubcategoria()),
                    () -> assertNull(entity.getFechaCreacion()),
                    () -> assertTrue(entity.isActivo()),
                    () -> assertEquals("Sensores", entity.getNombre()),
                    () -> assertEquals("Medición", entity.getDescripcion()),
                    () -> assertSame(padre, entity.getCategoria()));
            entity.setIdSubcategoria(7);
            entity.setFechaCreacion(FECHA_CREACION);
            return entity;
        });

        Subcategoria resultado = this.service.crearSubcategoria(entrada);

        assertAll(
                () -> assertEquals(7, resultado.getId()),
                () -> assertEquals(FECHA_CREACION, resultado.getFechaCreacion()),
                () -> assertTrue(resultado.isActivo()),
                () -> assertEquals(3, resultado.getCategoria().getId()),
                () -> assertEquals("Categoría 3", resultado.getCategoria().getNombre()));
        verify(this.categoriaRepository).findForUpdateByIdCategoriaAndActivoTrue(3);
        verify(this.subcategoriaRepository).existsByCategoria_IdCategoriaAndNombreIgnoreCase(3, "Sensores");
    }

    @Test
    void crearConCategoriaInexistenteNoGuardaLaSubcategoria() {
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(3)).thenReturn(Optional.empty());
        when(this.categoriaRepository.existsById(3)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> this.service.crearSubcategoria(entrada(3, "Sensores")));

        verifyNoInteractions(this.subcategoriaRepository);
    }

    @Test
    void crearConCategoriaInactivaProduceConflictoSinGuardar() {
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(3)).thenReturn(Optional.empty());
        when(this.categoriaRepository.existsById(3)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> this.service.crearSubcategoria(entrada(3, "Sensores")));

        assertEquals("La categoría seleccionada está inactiva.", exception.getMessage());
        verifyNoInteractions(this.subcategoriaRepository);
    }

    @Test
    void crearRechazaNombreReservadoDentroDelPadreIncluyendoSubcategoriasInactivas() {
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(3))
                .thenReturn(Optional.of(categoriaActiva(3)));
        // No se filtra por activo: tanto una activa como una baja reservan el nombre.
        when(this.subcategoriaRepository.existsByCategoria_IdCategoriaAndNombreIgnoreCase(3, "sensores"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> this.service.crearSubcategoria(entrada(3, "  sensores  ")));

        verify(this.subcategoriaRepository).existsByCategoria_IdCategoriaAndNombreIgnoreCase(3, "sensores");
        verifyNoMoreInteractions(this.subcategoriaRepository);
    }

    @Test
    void crearMismoNombreEnOtraCategoriaSoloCompruebaUnicidadDentroDelDestino() {
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(4))
                .thenReturn(Optional.of(categoriaActiva(4)));
        when(this.subcategoriaRepository.existsByCategoria_IdCategoriaAndNombreIgnoreCase(4, "Sensores"))
                .thenReturn(false);
        when(this.subcategoriaRepository.saveAndFlush(any(SubcategoriaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subcategoria resultado = this.service.crearSubcategoria(entrada(4, "Sensores"));

        assertEquals(4, resultado.getCategoria().getId());
        verify(this.subcategoriaRepository).existsByCategoria_IdCategoriaAndNombreIgnoreCase(4, "Sensores");
        verify(this.subcategoriaRepository).saveAndFlush(any(SubcategoriaEntity.class));
        verifyNoMoreInteractions(this.subcategoriaRepository);
    }

    @Test
    void listarUtilizaLaConsultaQueSoloRecuperaActivas() {
        when(this.subcategoriaRepository.findAllByActivoTrueOrderByIdSubcategoriaAsc())
                .thenReturn(List.of(subcategoriaActiva()));

        List<Subcategoria> resultado = this.service.listarSubcategorias();

        assertAll(
                () -> assertEquals(1, resultado.size()),
                () -> assertTrue(resultado.getFirst().isActivo()),
                () -> assertEquals(7, resultado.getFirst().getId()),
                () -> assertEquals(3, resultado.getFirst().getCategoria().getId()));
        verify(this.subcategoriaRepository).findAllByActivoTrueOrderByIdSubcategoriaAsc();
        verifyNoMoreInteractions(this.subcategoriaRepository);
    }

    @Test
    void obtenerSubcategoriaActivaIncluyeSuCategoria() {
        when(this.subcategoriaRepository.findByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(subcategoriaActiva()));

        Subcategoria resultado = this.service.obtenerSubcategoria(7);

        assertAll(
                () -> assertEquals(7, resultado.getId()),
                () -> assertEquals("Sensores", resultado.getNombre()),
                () -> assertTrue(resultado.isActivo()),
                () -> assertEquals(3, resultado.getCategoria().getId()));
    }

    @Test
    void listarPorCategoriaActivaRestringeLaConsultaAlPadre() {
        when(this.categoriaRepository.findByIdCategoriaAndActivoTrue(3))
                .thenReturn(Optional.of(categoriaActiva(3)));
        when(this.subcategoriaRepository.findAllByCategoria_IdCategoriaAndActivoTrueOrderByIdSubcategoriaAsc(3))
                .thenReturn(List.of(subcategoriaActiva()));

        List<Subcategoria> resultado = this.service.listarPorCategoria(3);

        assertAll(
                () -> assertEquals(1, resultado.size()),
                () -> assertEquals(3, resultado.getFirst().getCategoria().getId()));
        verify(this.subcategoriaRepository).findAllByCategoria_IdCategoriaAndActivoTrueOrderByIdSubcategoriaAsc(3);
        verifyNoMoreInteractions(this.subcategoriaRepository);
    }

    @Test
    void listarPorCategoriaAusenteOInactivaRechazaLaConsultaDeHijos() {
        when(this.categoriaRepository.findByIdCategoriaAndActivoTrue(3)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> this.service.listarPorCategoria(3));

        verifyNoInteractions(this.subcategoriaRepository);
    }

    @Test
    void subcategoriaAusenteOInactivaNoSePuedeConsultarActualizarNiEliminar() {
        when(this.subcategoriaRepository.findByIdSubcategoriaAndActivoTrue(7)).thenReturn(Optional.empty());
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7)).thenReturn(Optional.empty());

        assertAll(
                () -> assertThrows(ResourceNotFoundException.class, () -> this.service.obtenerSubcategoria(7)),
                () -> assertThrows(ResourceNotFoundException.class,
                        () -> this.service.actualizarSubcategoria(7, entrada(3, "Sensores"))),
                () -> assertThrows(ResourceNotFoundException.class, () -> this.service.eliminarSubcategoria(7)));

        verify(this.subcategoriaRepository).findByIdSubcategoriaAndActivoTrue(7);
        verify(this.subcategoriaRepository, times(2)).findForUpdateByIdSubcategoriaAndActivoTrue(7);
        verifyNoMoreInteractions(this.subcategoriaRepository);
        verifyNoInteractions(this.categoriaRepository);
    }

    @Test
    void actualizarConNombrePropioBorraDescripcionYConservaCamposDelServidor() {
        SubcategoriaEntity existente = subcategoriaActiva();
        Subcategoria cambios = entrada(3, "  sensores  ");
        cambios.setId(999);
        cambios.setActivo(false);
        cambios.setFechaCreacion(FECHA_CREACION.plusDays(1));
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(existente));
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(3))
                .thenReturn(Optional.of(existente.getCategoria()));
        when(this.subcategoriaRepository.saveAndFlush(existente)).thenReturn(existente);

        Subcategoria resultado = this.service.actualizarSubcategoria(7, cambios);

        assertAll(
                () -> assertEquals(7, resultado.getId()),
                () -> assertEquals("sensores", resultado.getNombre()),
                () -> assertNull(resultado.getDescripcion()),
                () -> assertTrue(resultado.isActivo()),
                () -> assertEquals(FECHA_CREACION, resultado.getFechaCreacion()),
                () -> assertEquals(3, resultado.getCategoria().getId()));
        verify(this.subcategoriaRepository)
                .existsByCategoria_IdCategoriaAndNombreIgnoreCaseAndIdSubcategoriaNot(3, "sensores", 7);
    }

    @Test
    void actualizarPermiteCambiarElPadreYNormalizaLosCamposEditables() {
        SubcategoriaEntity existente = subcategoriaActiva();
        CategoriaEntity destino = categoriaActiva(4);
        Subcategoria cambios = entrada(4, "  Sensores de posición  ");
        cambios.setDescripcion("  Nueva descripción  ");
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(existente));
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(4)).thenReturn(Optional.of(destino));
        when(this.subcategoriaRepository.saveAndFlush(existente)).thenReturn(existente);

        Subcategoria resultado = this.service.actualizarSubcategoria(7, cambios);

        assertAll(
                () -> assertSame(destino, existente.getCategoria()),
                () -> assertEquals(4, resultado.getCategoria().getId()),
                () -> assertEquals("Sensores de posición", resultado.getNombre()),
                () -> assertEquals("Nueva descripción", resultado.getDescripcion()),
                () -> assertTrue(resultado.isActivo()));
        verify(this.subcategoriaRepository)
                .existsByCategoria_IdCategoriaAndNombreIgnoreCaseAndIdSubcategoriaNot(4, "Sensores de posición", 7);
    }

    @Test
    void actualizarHaciaCategoriaInexistenteNoModificaLaSubcategoria() {
        SubcategoriaEntity existente = subcategoriaActiva();
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(existente));
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(4)).thenReturn(Optional.empty());
        when(this.categoriaRepository.existsById(4)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> this.service.actualizarSubcategoria(7, entrada(4, "Nombre nuevo")));

        assertEquals(3, existente.getCategoria().getIdCategoria());
        assertEquals("Sensores", existente.getNombre());
        verify(this.subcategoriaRepository, never()).saveAndFlush(any(SubcategoriaEntity.class));
    }

    @Test
    void actualizarHaciaCategoriaInactivaNoModificaLaSubcategoria() {
        SubcategoriaEntity existente = subcategoriaActiva();
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(existente));
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(4)).thenReturn(Optional.empty());
        when(this.categoriaRepository.existsById(4)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> this.service.actualizarSubcategoria(7, entrada(4, "Nombre nuevo")));

        assertEquals(3, existente.getCategoria().getIdCategoria());
        assertEquals("Sensores", existente.getNombre());
        verify(this.subcategoriaRepository, never()).saveAndFlush(any(SubcategoriaEntity.class));
    }

    @Test
    void actualizarRechazaDuplicadoEnDestinoAntesDeCambiarLaEntidad() {
        SubcategoriaEntity existente = subcategoriaActiva();
        CategoriaEntity padreOriginal = existente.getCategoria();
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(existente));
        when(this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(4))
                .thenReturn(Optional.of(categoriaActiva(4)));
        when(this.subcategoriaRepository.existsByCategoria_IdCategoriaAndNombreIgnoreCaseAndIdSubcategoriaNot(
                4, "instrumentos", 7)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> this.service.actualizarSubcategoria(7, entrada(4, "  instrumentos  ")));

        assertAll(
                () -> assertSame(padreOriginal, existente.getCategoria()),
                () -> assertEquals("Sensores", existente.getNombre()),
                () -> assertEquals("Medición", existente.getDescripcion()),
                () -> assertTrue(existente.isActivo()));
        verify(this.subcategoriaRepository, never()).saveAndFlush(any(SubcategoriaEntity.class));
    }

    @Test
    void eliminarMarcaInactivaLaMismaEntidadSinBorrarlaFisicamente() {
        SubcategoriaEntity existente = subcategoriaActiva();
        CategoriaEntity padreOriginal = existente.getCategoria();
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(existente));
        when(this.subcategoriaRepository.saveAndFlush(existente)).thenAnswer(invocation -> {
            assertSame(existente, invocation.getArgument(0));
            assertFalse(existente.isActivo());
            return existente;
        });

        this.service.eliminarSubcategoria(7);

        assertAll(
                () -> assertFalse(existente.isActivo()),
                () -> assertEquals(7, existente.getIdSubcategoria()),
                () -> assertEquals("Sensores", existente.getNombre()),
                () -> assertEquals(FECHA_CREACION, existente.getFechaCreacion()),
                () -> assertSame(padreOriginal, existente.getCategoria()));
        verify(this.subcategoriaRepository).findForUpdateByIdSubcategoriaAndActivoTrue(7);
        verify(this.subcategoriaRepository).saveAndFlush(existente);
        verifyNoMoreInteractions(this.subcategoriaRepository);
    }

    @Test
    void segundoDeleteRechazaLaSubcategoriaQueYaNoEstaActiva() {
        SubcategoriaEntity existente = subcategoriaActiva();
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7))
                .thenReturn(Optional.of(existente), Optional.empty());

        this.service.eliminarSubcategoria(7);
        assertThrows(ResourceNotFoundException.class, () -> this.service.eliminarSubcategoria(7));

        assertFalse(existente.isActivo());
        verify(this.subcategoriaRepository).saveAndFlush(existente);
        verify(this.subcategoriaRepository, times(2)).findForUpdateByIdSubcategoriaAndActivoTrue(7);
        verifyNoMoreInteractions(this.subcategoriaRepository);
    }

    private Subcategoria entrada(Integer idCategoria, String nombre) {
        return Subcategoria.builder().nombre(nombre).categoria(Categoria.builder().id(idCategoria).build()).build();
    }

    @Test
    void equiposNoBajaImpidenDesactivarYConSoloBajaSePermite() {
        SubcategoriaEntity entity = subcategoriaActiva();
        when(this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(7)).thenReturn(Optional.of(entity));
        when(this.equipoRepository.existsBySubcategoria_IdSubcategoriaAndEstadoNot(7, EstadoEquipo.BAJA)).thenReturn(true, false);
        assertThrows(ConflictException.class, () -> this.service.eliminarSubcategoria(7));
        assertTrue(entity.isActivo());
        verify(this.subcategoriaRepository, never()).saveAndFlush(any());
        this.service.eliminarSubcategoria(7);
        assertFalse(entity.isActivo());
        verify(this.subcategoriaRepository).saveAndFlush(entity);
    }

    private CategoriaEntity categoriaActiva(Integer id) {
        return CategoriaEntity.builder().idCategoria(id).nombre("Categoría " + id)
                .activo(true).fechaCreacion(FECHA_CREACION).build();
    }

    private SubcategoriaEntity subcategoriaActiva() {
        return SubcategoriaEntity.builder().idSubcategoria(7).nombre("Sensores").descripcion("Medición")
                .activo(true).fechaCreacion(FECHA_CREACION).categoria(categoriaActiva(3)).build();
    }
}
