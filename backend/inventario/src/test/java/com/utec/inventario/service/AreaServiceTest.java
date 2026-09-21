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
import com.utec.inventario.domain.Sede;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.SedeEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.AreaMapper;
import com.utec.inventario.repository.AreaRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.SedeRepository;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {
    @Mock private AreaRepository areas;
    @Mock private SedeRepository sedes;
    @Mock private LaboratorioRepository laboratorios;
    private AreaService service;

    @BeforeEach
    void preparar() { service = new AreaService(areas, sedes, laboratorios, Mappers.getMapper(AreaMapper.class)); }

    @Test
    void padreAusenteOInactivoSeDistingueAntesDePersistir() {
        when(sedes.findForUpdateByIdSedeAndActivoTrue(2)).thenReturn(Optional.empty());
        when(sedes.existsById(2)).thenReturn(false, true);
        assertThrows(ResourceNotFoundException.class, () -> service.crearArea(input(2, "Área")));
        assertThrows(ConflictException.class, () -> service.crearArea(input(2, "Área")));
        verifyNoInteractions(areas, laboratorios);
    }

    @Test
    void moverConDuplicadoRechazaAntesDeAlterarLaEntidad() {
        SedeEntity original = SedeEntity.builder().idSede(2).nombre("Lima").activo(true).build();
        SedeEntity destination = SedeEntity.builder().idSede(3).nombre("Otra").activo(true).build();
        AreaEntity entity = AreaEntity.builder().idArea(7).nombre("Original").sede(original).activo(true).build();
        when(areas.findForUpdateByIdAreaAndActivoTrue(7)).thenReturn(Optional.of(entity));
        when(sedes.findForUpdateByIdSedeAndActivoTrue(3)).thenReturn(Optional.of(destination));
        when(areas.existsBySede_IdSedeAndNombreIgnoreCaseAndIdAreaNot(3, "Destino", 7)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.actualizarArea(7, input(3, "  Destino  ")));
        assertSame(original, entity.getSede());
        assertEquals("Original", entity.getNombre());
        assertTrue(entity.isActivo());
        verify(areas, never()).saveAndFlush(any());
    }

    @Test
    void moverAsignaPadreValidadoYConservaIdentidadFechaYEstado() {
        OffsetDateTime date = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        AreaEntity entity = AreaEntity.builder().idArea(7).nombre("Original").descripcion("Antes")
                .sede(SedeEntity.builder().idSede(2).build()).activo(true).fechaCreacion(date).build();
        SedeEntity destination = SedeEntity.builder().idSede(3).nombre("Otra").activo(true).build();
        when(areas.findForUpdateByIdAreaAndActivoTrue(7)).thenReturn(Optional.of(entity));
        when(sedes.findForUpdateByIdSedeAndActivoTrue(3)).thenReturn(Optional.of(destination));
        when(areas.saveAndFlush(entity)).thenReturn(entity);
        Area changes = input(3, "  Actualizada  ");
        changes.setId(999);
        changes.setFechaCreacion(date.plusDays(1));
        changes.setActivo(false);
        Area result = service.actualizarArea(7, changes);
        assertSame(destination, entity.getSede());
        assertEquals(7, result.getId());
        assertEquals("Actualizada", result.getNombre());
        assertNull(result.getDescripcion());
        assertTrue(result.isActivo());
        assertEquals(date, result.getFechaCreacion());
        verify(areas).existsBySede_IdSedeAndNombreIgnoreCaseAndIdAreaNot(3, "Actualizada", 7);
    }

    @Test
    void bajaConLaboratoriosActivosConservaPadreYDespuesPermiteBajaLogica() {
        AreaEntity entity = AreaEntity.builder().idArea(7).nombre("Área").activo(true).build();
        when(areas.findForUpdateByIdAreaAndActivoTrue(7)).thenReturn(Optional.of(entity));
        when(laboratorios.existsByArea_IdAreaAndActivoTrue(7)).thenReturn(true, false);
        assertThrows(ConflictException.class, () -> service.eliminarArea(7));
        assertTrue(entity.isActivo());
        verify(areas, never()).saveAndFlush(any());
        service.eliminarArea(7);
        assertFalse(entity.isActivo());
        verify(areas).saveAndFlush(entity);
        verify(areas, never()).delete(any());
    }

    private Area input(int parent, String name) {
        return Area.builder().nombre(name).sede(Sede.builder().id(parent).build()).build();
    }
}
