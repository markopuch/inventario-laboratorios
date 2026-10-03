package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.TipoMantenimiento;
import com.utec.inventario.dto.request.CreateMantenimientoRequest;
import com.utec.inventario.dto.request.UpdateMantenimientoRequest;

class MantenimientoMapperTest {
    private final MantenimientoMapper mapper = Mappers.getMapper(MantenimientoMapper.class);

    @Test
    void requestMapeaIdsSinFechasEstadosGeneradosNiCredenciales() {
        var request = CreateMantenimientoRequest.builder().idEquipo(7).idResponsable(3)
                .tipo(TipoMantenimiento.CORRECTIVO).descripcion("Revisión")
                .fechaProgramada(LocalDate.of(2026, 10, 10)).build();
        var domain = mapper.convert(request);
        assertEquals(7, domain.getEquipo().getId());
        assertEquals(3, domain.getResponsable().getId());
        assertNull(domain.getEstado());
        assertNull(domain.getFechaInicio()); assertNull(domain.getFechaFin());
        assertNull(domain.getFechaCreacion()); assertNull(domain.getId());
        var entity = mapper.toEntity(domain);
        assertNull(entity.getEquipo()); assertNull(entity.getResponsable());
    }

    @Test
    void putNoMapeaEquipoNiEstado() {
        var domain = mapper.convert(UpdateMantenimientoRequest.builder().tipo(TipoMantenimiento.OTRO)
                .descripcion("Revisión").fechaProgramada(LocalDate.of(2026, 10, 11)).build());
        assertNull(domain.getEquipo()); assertNull(domain.getEstado());
        assertNull(domain.getFechaInicio()); assertNull(domain.getFechaFin());
    }
}
