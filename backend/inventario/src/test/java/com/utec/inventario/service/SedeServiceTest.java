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

import com.utec.inventario.domain.Sede;
import com.utec.inventario.entity.SedeEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.SedeMapper;
import com.utec.inventario.repository.AreaRepository;
import com.utec.inventario.repository.SedeRepository;

@ExtendWith(MockitoExtension.class)
class SedeServiceTest {
    @Mock private SedeRepository sedes;
    @Mock private AreaRepository areas;
    private SedeService service;

    @BeforeEach
    void preparar() { service = new SedeService(sedes, areas, Mappers.getMapper(SedeMapper.class)); }

    @Test
    void crearNormalizaCamposYReiniciaCamposControladosPorServidor() {
        when(sedes.saveAndFlush(any(SedeEntity.class))).thenAnswer(call -> call.getArgument(0));
        Sede input = Sede.builder().id(999).nombre("  Lima  ").direccion("  Dirección  ")
                .distrito(" Barranco ").departamento(" Lima ").activo(false)
                .fechaCreacion(OffsetDateTime.now()).build();
        Sede result = service.crearSede(input);
        assertNull(result.getId());
        assertNull(result.getFechaCreacion());
        assertTrue(result.isActivo());
        assertEquals("Lima", result.getNombre());
        assertEquals("Dirección", result.getDireccion());
        assertEquals("Barranco", result.getDistrito());
        assertEquals("Lima", result.getDepartamento());
        verify(sedes).saveAndFlush(any(SedeEntity.class));
        verifyNoMoreInteractions(sedes);
        verifyNoInteractions(areas);
    }

    @Test
    void bajaConAreasActivasNoModificaElPadreYConSoloInactivasHaceBajaLogica() {
        SedeEntity entity = SedeEntity.builder().idSede(2).nombre("Lima").activo(true).build();
        when(sedes.findForUpdateByIdSedeAndActivoTrue(2)).thenReturn(Optional.of(entity));
        when(areas.existsBySede_IdSedeAndActivoTrue(2)).thenReturn(true, false);
        assertThrows(ConflictException.class, () -> service.eliminarSede(2));
        assertTrue(entity.isActivo());
        verify(sedes, never()).saveAndFlush(any());
        service.eliminarSede(2);
        assertFalse(entity.isActivo());
        verify(sedes).saveAndFlush(entity);
        verify(sedes, never()).delete(any());
    }

    @Test
    void sedeInactivaNoSeConsultaNiActualizaNiSeVuelveADesactivar() {
        when(sedes.findByIdSedeAndActivoTrue(2)).thenReturn(Optional.empty());
        when(sedes.findForUpdateByIdSedeAndActivoTrue(2)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSede(2));
        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizarSede(2, Sede.builder().nombre("Cambio").build()));
        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSede(2));
        verify(sedes, never()).saveAndFlush(any());
        verifyNoInteractions(areas);
    }
}
