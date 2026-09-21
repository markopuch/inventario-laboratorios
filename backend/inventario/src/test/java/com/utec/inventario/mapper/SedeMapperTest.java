package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.Sede;
import com.utec.inventario.dto.request.CreateSedeRequest;
import com.utec.inventario.dto.request.UpdateSedeRequest;
import com.utec.inventario.entity.SedeEntity;

class SedeMapperTest {
    private final SedeMapper mapper = Mappers.getMapper(SedeMapper.class);

    @Test
    void solicitudesNoControlanIdentidadEstadoNiFechaYRespuestaConservaLosDatosPersistidos() {
        for (Sede domain : List.of(
                mapper.convert(new CreateSedeRequest("Lima", "Dirección", "Barranco", "Lima")),
                mapper.convert(new UpdateSedeRequest("Lima", "Dirección", "Barranco", "Lima")))) {
            assertNull(domain.getId());
            assertFalse(domain.isActivo());
            assertNull(domain.getFechaCreacion());
            assertEquals("Barranco", domain.getDistrito());
        }
        OffsetDateTime date = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        SedeEntity entity = SedeEntity.builder().idSede(2).nombre("Lima").direccion("Dirección")
                .distrito("Barranco").departamento("Lima").activo(true).fechaCreacion(date).build();
        Sede domain = mapper.convert(entity);
        var response = mapper.toResponse(mapper.convert(List.of(entity))).getFirst();
        assertEquals(2, domain.getId());
        assertEquals(2, mapper.toEntity(domain).getIdSede());
        assertEquals(2, response.getId());
        assertTrue(response.isActivo());
        assertEquals(date, response.getFechaCreacion());
        assertEquals("Dirección", response.getDireccion());
    }

    @Test
    void putCompletoLimpiaOpcionalesPeroPreservaCamposDelServidor() {
        OffsetDateTime date = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        SedeEntity entity = SedeEntity.builder().idSede(2).nombre("Original").direccion("Antes")
                .distrito("Antes").departamento("Antes").activo(true).fechaCreacion(date).build();
        Sede changes = Sede.builder().id(999).nombre("Nueva").activo(false)
                .fechaCreacion(date.plusDays(1)).build();
        assertSame(entity, mapper.copy(entity, changes));
        assertEquals(2, entity.getIdSede());
        assertTrue(entity.isActivo());
        assertEquals(date, entity.getFechaCreacion());
        assertEquals("Nueva", entity.getNombre());
        assertNull(entity.getDireccion());
        assertNull(entity.getDistrito());
        assertNull(entity.getDepartamento());
    }
}
