package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.Area;
import com.utec.inventario.domain.Sede;
import com.utec.inventario.dto.request.CreateAreaRequest;
import com.utec.inventario.dto.request.UpdateAreaRequest;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.SedeEntity;

class AreaMapperTest {
    private final AreaMapper mapper = Mappers.getMapper(AreaMapper.class);

    @Test
    void solicitudReferenciaPadreDeDominioYRespuestaUsaResumenSinEntity() {
        for (Area domain : List.of(mapper.convert(new CreateAreaRequest("Mecatrónica", "Curso", 2)),
                mapper.convert(new UpdateAreaRequest("Mecatrónica", "Curso", 2)))) {
            assertEquals(2, domain.getSede().getId());
            assertNull(domain.getSede().getNombre());
            assertNull(domain.getId());
            assertFalse(domain.isActivo());
            assertNull(domain.getFechaCreacion());
            assertNull(mapper.toEntity(domain).getSede(), "El Service asigna el padre validado");
        }
        SedeEntity parent = SedeEntity.builder().idSede(2).nombre("Lima").activo(true).build();
        AreaEntity entity = AreaEntity.builder().idArea(7).nombre("Mecatrónica").sede(parent)
                .activo(true).fechaCreacion(OffsetDateTime.parse("2026-01-10T15:00:00Z")).build();
        Area domain = mapper.convert(entity);
        var response = mapper.toResponse(mapper.convert(List.of(entity))).getFirst();
        assertEquals(2, domain.getSede().getId());
        assertEquals(7, response.getId());
        assertEquals("Lima", response.getSede().getNombre());
        assertEquals(2, response.getSede().getId());
        assertEquals(entity.getFechaCreacion(), response.getFechaCreacion());
    }

    @Test
    void copiarNoAceptaCambiosDeServidorNiSustituyePadreNoValidado() {
        SedeEntity parent = SedeEntity.builder().idSede(2).nombre("Lima").build();
        OffsetDateTime date = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        AreaEntity entity = AreaEntity.builder().idArea(7).nombre("Anterior").descripcion("Anterior")
                .sede(parent).activo(true).fechaCreacion(date).build();
        Area changes = Area.builder().id(999).nombre("Actualizada").activo(false)
                .fechaCreacion(date.plusDays(1)).sede(Sede.builder().id(3).build()).build();
        assertSame(entity, mapper.copy(entity, changes));
        assertSame(parent, entity.getSede());
        assertEquals(7, entity.getIdArea());
        assertTrue(entity.isActivo());
        assertEquals(date, entity.getFechaCreacion());
        assertEquals("Actualizada", entity.getNombre());
        assertNull(entity.getDescripcion());
    }
}
