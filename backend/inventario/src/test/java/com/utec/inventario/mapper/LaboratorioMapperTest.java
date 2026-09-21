package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.Area;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.dto.request.CreateLaboratorioRequest;
import com.utec.inventario.dto.request.UpdateLaboratorioRequest;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.SedeEntity;

class LaboratorioMapperTest {
    private final LaboratorioMapper mapper = Mappers.getMapper(LaboratorioMapper.class);

    @Test
    void solicitudReferenciaAreaYRespuestaControlaElResumenDeLaJerarquia() {
        for (Laboratorio domain : List.of(
                mapper.convert(new CreateLaboratorioRequest("Robótica", "L500", "Piso 5", 7)),
                mapper.convert(new UpdateLaboratorioRequest("Robótica", "L500", "Piso 5", 7)))) {
            assertEquals(7, domain.getArea().getId());
            assertNull(domain.getId());
            assertFalse(domain.isActivo());
            assertNull(domain.getFechaCreacion());
            assertNull(mapper.toEntity(domain).getArea(), "El Mapper no resuelve relaciones persistentes");
        }
        SedeEntity sede = SedeEntity.builder().idSede(2).nombre("Lima").build();
        AreaEntity area = AreaEntity.builder().idArea(7).nombre("Mecatrónica").sede(sede).build();
        LaboratorioEntity entity = LaboratorioEntity.builder().idLaboratorio(9).nombre("Robótica")
                .codigo("L500").ubicacion("Piso 5").area(area).activo(true)
                .fechaCreacion(OffsetDateTime.parse("2026-01-10T15:00:00Z")).build();
        Laboratorio domain = mapper.convert(entity);
        var response = mapper.toResponse(mapper.convert(List.of(entity))).getFirst();
        assertEquals(2, domain.getArea().getSede().getId());
        assertEquals(9, response.getId());
        assertEquals("L500", response.getCodigo());
        assertEquals(7, response.getArea().getId());
        assertEquals("Mecatrónica", response.getArea().getNombre());
        assertEquals(entity.getFechaCreacion(), response.getFechaCreacion());
    }

    @Test
    void copiarLimpiaUbicacionYConservaIdentidadEstadoFechaYAreaPersistente() {
        AreaEntity parent = AreaEntity.builder().idArea(7).nombre("Mecatrónica").build();
        OffsetDateTime date = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        LaboratorioEntity entity = LaboratorioEntity.builder().idLaboratorio(9).nombre("Original")
                .codigo("L500").ubicacion("Piso 5").area(parent).activo(true).fechaCreacion(date).build();
        Laboratorio changes = Laboratorio.builder().id(999).nombre("Nuevo").codigo("L501")
                .activo(false).fechaCreacion(date.plusDays(1)).area(Area.builder().id(8).build()).build();
        assertSame(entity, mapper.copy(entity, changes));
        assertSame(parent, entity.getArea());
        assertEquals(9, entity.getIdLaboratorio());
        assertTrue(entity.isActivo());
        assertEquals(date, entity.getFechaCreacion());
        assertEquals("L501", entity.getCodigo());
        assertEquals("Nuevo", entity.getNombre());
        assertNull(entity.getUbicacion());
    }
}
