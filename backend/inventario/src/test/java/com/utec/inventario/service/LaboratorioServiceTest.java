package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utec.inventario.domain.Area;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.repository.AreaRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.UsuarioLaboratorioRepository;
import com.utec.inventario.repository.EquipoRepository;

@ExtendWith(MockitoExtension.class)
class LaboratorioServiceTest {
    @Mock private LaboratorioRepository laboratorios;
    @Mock private AreaRepository areas;
    @Mock private UsuarioLaboratorioRepository asignaciones;
    @Mock private EquipoRepository equipos;
    private LaboratorioService service;

    @BeforeEach
    void preparar() { service = new LaboratorioService(laboratorios, areas, Mappers.getMapper(LaboratorioMapper.class), asignaciones, equipos); }

    @Test
    void areaAusenteOInactivaSeDistingueAntesDePersistir() {
        when(areas.findForUpdateByIdAreaAndActivoTrue(7)).thenReturn(Optional.empty());
        when(areas.existsById(7)).thenReturn(false, true);
        assertThrows(ResourceNotFoundException.class, () -> service.crearLaboratorio(input(7, "L500")));
        assertThrows(ConflictException.class, () -> service.crearLaboratorio(input(7, "L500")));
        verifyNoInteractions(laboratorios);
    }

    @Test
    void codigoGlobalDuplicadoBloqueaCreacionYCambioSinModificarEntidad() {
        AreaEntity area = AreaEntity.builder().idArea(7).activo(true).build();
        LaboratorioEntity entity = LaboratorioEntity.builder().idLaboratorio(9).nombre("Original")
                .codigo("L500").area(area).activo(true).build();
        when(areas.findForUpdateByIdAreaAndActivoTrue(7)).thenReturn(Optional.of(area));
        when(laboratorios.existsByCodigoIgnoreCase("l501")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.crearLaboratorio(input(7, "  l501  ")));
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9)).thenReturn(Optional.of(entity));
        when(laboratorios.existsByCodigoIgnoreCaseAndIdLaboratorioNot("l501", 9)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.actualizarLaboratorio(9, input(7, "  l501  ")));
        assertEquals("L500", entity.getCodigo());
        assertEquals("Original", entity.getNombre());
        verify(laboratorios, never()).saveAndFlush(any());
    }

    @Test
    void moverMantieneCodigoPropioYCamposDelServidorLimpiandoUbicacionOmitida() {
        OffsetDateTime date = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        LaboratorioEntity entity = LaboratorioEntity.builder().idLaboratorio(9).nombre("Original")
                .codigo("L500").ubicacion("Antes").area(AreaEntity.builder().idArea(7).build())
                .activo(true).fechaCreacion(date).build();
        AreaEntity destination = AreaEntity.builder().idArea(8).nombre("Destino").activo(true).build();
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9)).thenReturn(Optional.of(entity));
        when(areas.findForUpdateByIdAreaAndActivoTrue(8)).thenReturn(Optional.of(destination));
        when(laboratorios.saveAndFlush(entity)).thenReturn(entity);
        Laboratorio changes = input(8, "  L500  ");
        changes.setNombre("  Robótica  ");
        changes.setId(999);
        changes.setFechaCreacion(date.plusDays(1));
        Laboratorio result = service.actualizarLaboratorio(9, changes);
        assertSame(destination, entity.getArea());
        assertEquals(9, result.getId());
        assertTrue(result.isActivo());
        assertEquals(date, result.getFechaCreacion());
        assertEquals("L500", result.getCodigo());
        assertEquals("Robótica", result.getNombre());
        assertNull(result.getUbicacion());
        verify(laboratorios).existsByCodigoIgnoreCaseAndIdLaboratorioNot("L500", 9);
    }

    @Test
    void bajaEsLogicaYSegundoIntentoNoReviveNiBorraRegistro() {
        LaboratorioEntity entity = LaboratorioEntity.builder().idLaboratorio(9).codigo("L500")
                .nombre("Laboratorio").activo(true).build();
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9))
                .thenReturn(Optional.of(entity), Optional.empty());
        service.eliminarLaboratorio(9);
        assertFalse(entity.isActivo());
        assertThrows(ResourceNotFoundException.class, () -> service.eliminarLaboratorio(9));
        verify(laboratorios).saveAndFlush(entity);
        verify(laboratorios, never()).delete(any());
        verifyNoInteractions(areas);
    }

    @Test
    void asignacionesActivasBloqueanBajaBajoElMismoBloqueoDelLaboratorio() {
        LaboratorioEntity entity = LaboratorioEntity.builder().idLaboratorio(9).codigo("L500")
                .nombre("Laboratorio").activo(true).build();
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9)).thenReturn(Optional.of(entity));
        when(asignaciones.existsByLaboratorio_IdLaboratorioAndActivoTrue(9)).thenReturn(true, false);
        assertThrows(ConflictException.class, () -> service.eliminarLaboratorio(9));
        assertTrue(entity.isActivo());
        verify(laboratorios, never()).saveAndFlush(any());
        var ordered = inOrder(laboratorios, asignaciones);
        ordered.verify(laboratorios).findForUpdateByIdLaboratorioAndActivoTrue(9);
        ordered.verify(asignaciones).existsByLaboratorio_IdLaboratorioAndActivoTrue(9);
        service.eliminarLaboratorio(9);
        assertFalse(entity.isActivo());
        verify(laboratorios).saveAndFlush(entity);
        verify(laboratorios, never()).delete(any());
    }

    private Laboratorio input(int parent, String code) {
        return Laboratorio.builder().nombre("Laboratorio").codigo(code)
                .area(Area.builder().id(parent).build()).build();
    }

    @Test
    void equiposNoBajaBloqueanAunqueNoHayaAsignacionesYSoloBajaPermiteDesactivar() {
        LaboratorioEntity entity = LaboratorioEntity.builder().idLaboratorio(9).codigo("L500").activo(true).build();
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9)).thenReturn(Optional.of(entity));
        when(equipos.existsByLaboratorio_IdLaboratorioAndEstadoNot(9, EstadoEquipo.BAJA)).thenReturn(true, false);
        assertThrows(ConflictException.class, () -> service.eliminarLaboratorio(9));
        assertTrue(entity.isActivo());
        verify(laboratorios, never()).saveAndFlush(any());
        service.eliminarLaboratorio(9);
        assertFalse(entity.isActivo());
        verify(laboratorios).saveAndFlush(entity);
    }
}
