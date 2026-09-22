package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.MovimientoEquipoEntity;
import com.utec.inventario.entity.UsuarioEntity;

class MovimientoEquipoMapperTest {
    private final MovimientoEquipoMapper mapper = Mappers.getMapper(MovimientoEquipoMapper.class);

    @Test
    void convierteEventoConResumenenesPublicosSinRecorrerCredencialesNiModificarEquipo() {
        UsuarioEntity usuarioJpa = mock(UsuarioEntity.class);
        EquipoEntity equipo = EquipoEntity.builder().idEquipo(7).codigoInterno("EQ-7").nombre("Osciloscopio").build();
        LaboratorioEntity origen = LaboratorioEntity.builder().idLaboratorio(2).codigo("L201").nombre("Origen").build();
        LaboratorioEntity destino = LaboratorioEntity.builder().idLaboratorio(3).codigo("L206").nombre("Destino").build();
        OffsetDateTime fecha = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        MovimientoEquipoEntity entity = MovimientoEquipoEntity.builder().idMovimiento(9).equipo(equipo)
                .laboratorioOrigen(origen).laboratorioDestino(destino).usuarioActor(usuarioJpa)
                .tipoMovimiento("TRASLADO").motivo("Curso").fechaMovimiento(fecha).build();
        Usuario actor = Usuario.builder().id(4).userName("gestor").nombre("Nombre").apellido("Apellido")
                .email("privado@example.invalid").rol("GESTOR").build();
        var domain = mapper.convert(entity, actor);
        var response = mapper.toResponse(domain);
        assertEquals(9, domain.getId());
        assertEquals(7, response.getEquipo().getId());
        assertEquals("EQ-7", response.getEquipo().getCodigoInterno());
        assertEquals(2, response.getLaboratorioOrigen().getId());
        assertEquals(3, response.getLaboratorioDestino().getId());
        assertEquals(4, response.getActor().getId());
        assertEquals("gestor", response.getActor().getUserName());
        assertEquals("TRASLADO", response.getTipoMovimiento());
        assertEquals("Curso", response.getMotivo());
        assertEquals(fecha, response.getFechaMovimiento());
        assertNull(domain.getEquipo().getResponsable(), "El evento solo contiene un resumen del equipo");
        assertNull(domain.getEquipo().getLaboratorio());
        assertSame(equipo, entity.getEquipo());
        assertNull(equipo.getLaboratorio(), "Mapear no efectúa el traslado");
        verifyNoInteractions(usuarioJpa);
    }

    @Test
    void listasPreservanOrdenFechaYOrigenNullableSinCerrarLosStringsPermitidosPorV3() {
        MovimientoEquipoEntity entity = MovimientoEquipoEntity.builder().idMovimiento(9)
                .equipo(EquipoEntity.builder().idEquipo(7).nombre("Equipo").codigoInterno("EQ-7").build())
                .laboratorioOrigen(null).laboratorioDestino(LaboratorioEntity.builder().idLaboratorio(3).build())
                .tipoMovimiento("VALOR_HISTORICO").motivo("Dato válido según CHECK de V3")
                .fechaMovimiento(OffsetDateTime.parse("2026-01-10T15:00:00Z")).build();
        var responses = mapper.toResponse(mapper.convert(List.of(entity)));
        assertEquals(1, responses.size());
        assertEquals(9, responses.getFirst().getId());
        assertNull(responses.getFirst().getLaboratorioOrigen());
        assertEquals(3, responses.getFirst().getLaboratorioDestino().getId());
        assertEquals("VALOR_HISTORICO", responses.getFirst().getTipoMovimiento());
        assertEquals(entity.getFechaMovimiento(), responses.getFirst().getFechaMovimiento());
        assertNull(responses.getFirst().getActor(), "El Service debe aportar la proyección pública del actor");
    }
}
