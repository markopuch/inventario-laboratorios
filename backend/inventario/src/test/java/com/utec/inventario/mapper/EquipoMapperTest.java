package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.dto.request.CreateEquipoRequest;
import com.utec.inventario.dto.request.UpdateEquipoRequest;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.SubcategoriaEntity;
import com.utec.inventario.entity.UsuarioEntity;

class EquipoMapperTest {
    private final EquipoMapper mapper = Mappers.getMapper(EquipoMapper.class);

    @Test
    void requestsDiferencianAltaDeEdicionYNoResuelvenRelacionesPersistentes() {
        CreateEquipoRequest creation = CreateEquipoRequest.builder().codigoInterno("EQ-1").nombre("Osciloscopio")
                .estado(EstadoEquipo.OPERATIVO).requiereMantenimiento(false).idSubcategoria(2).idLaboratorio(3)
                .idResponsable(4).build();
        Equipo domain = mapper.convert(creation);
        assertNull(domain.getId());
        assertNull(domain.getFechaCreacion());
        assertNull(domain.getFechaActualizacion());
        assertEquals("EQ-1", domain.getCodigoInterno());
        assertEquals(2, domain.getSubcategoria().getId());
        assertEquals(3, domain.getLaboratorio().getId());
        assertEquals(4, domain.getResponsable().getId());
        EquipoEntity entity = mapper.toEntity(domain);
        assertNull(entity.getSubcategoria());
        assertNull(entity.getLaboratorio());
        assertNull(entity.getResponsable());
        UpdateEquipoRequest update = UpdateEquipoRequest.builder().nombre("Actualizado").estado(EstadoEquipo.MANTENIMIENTO)
                .requiereMantenimiento(true).idSubcategoria(5).build();
        Equipo changed = mapper.convert(update);
        assertNull(changed.getCodigoInterno());
        assertNull(changed.getLaboratorio());
        assertNull(changed.getResponsable());
        assertNull(changed.getId());
        assertNull(changed.getFechaCreacion());
        assertNull(changed.getFechaActualizacion());
        assertEquals(5, changed.getSubcategoria().getId());
        assertTrue(changed.isRequiereMantenimiento());
    }

    @Test
    void respuestaUsaResponsablePublicoSinRecorrerUsuarioJpaYAdmiteNull() {
        UsuarioEntity privateEntity = mock(UsuarioEntity.class);
        EquipoEntity entity = EquipoEntity.builder().idEquipo(7).codigoInterno("EQ-1").nombre("Equipo")
                .estado(EstadoEquipo.OPERATIVO).subcategoria(SubcategoriaEntity.builder().idSubcategoria(2).nombre("Osciloscopios").build())
                .laboratorio(LaboratorioEntity.builder().idLaboratorio(3).codigo("L201").nombre("Electrónica").build())
                .responsable(privateEntity).fechaCreacion(OffsetDateTime.parse("2026-01-10T15:00:00Z")).build();
        Usuario publicUser = Usuario.builder().id(4).userName("lector").nombre("Nombre").apellido("Apellido").rol("LECTOR").build();
        var response = mapper.toResponse(mapper.convert(entity, publicUser));
        assertEquals(7, response.getId());
        assertEquals("EQ-1", response.getCodigoInterno());
        assertEquals(2, response.getSubcategoria().getId());
        assertEquals(3, response.getLaboratorio().getId());
        assertEquals("L201", response.getLaboratorio().getCodigo());
        assertEquals(4, response.getResponsable().getId());
        assertEquals("lector", response.getResponsable().getUserName());
        assertNull(mapper.toResponse(mapper.convert(entity)).getResponsable());
        assertNull(mapper.toResponse(mapper.convert(entity, null)).getResponsable());
        verifyNoInteractions(privateEntity);
    }

    @Test
    void copyConservaCodigoLaboratorioIdFechasYRelacionesHastaQueServiceLasValide() {
        OffsetDateTime date = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        SubcategoriaEntity sub = SubcategoriaEntity.builder().idSubcategoria(2).build();
        LaboratorioEntity lab = LaboratorioEntity.builder().idLaboratorio(3).build();
        UsuarioEntity user = UsuarioEntity.builder().idUsuario(4).build();
        EquipoEntity entity = EquipoEntity.builder().idEquipo(7).codigoInterno("EQ-1").nombre("Anterior")
                .marca("Marca").estado(EstadoEquipo.OPERATIVO).subcategoria(sub).laboratorio(lab).responsable(user)
                .fechaCreacion(date).fechaActualizacion(date).build();
        Equipo malicious = Equipo.builder().id(999).codigoInterno("CAMBIAR").nombre("Nuevo").estado(EstadoEquipo.INOPERATIVO)
                .requiereMantenimiento(true).fechaCreacion(date.plusDays(1)).fechaActualizacion(date.plusDays(1))
                .subcategoria(Subcategoria.builder().id(20).build()).laboratorio(Laboratorio.builder().id(30).build())
                .responsable(Usuario.builder().id(40).build()).build();
        mapper.copy(entity, malicious);
        assertEquals(7, entity.getIdEquipo());
        assertEquals("EQ-1", entity.getCodigoInterno());
        assertEquals(date, entity.getFechaCreacion());
        assertEquals(date, entity.getFechaActualizacion());
        assertSame(sub, entity.getSubcategoria());
        assertSame(lab, entity.getLaboratorio());
        assertSame(user, entity.getResponsable());
        assertNull(entity.getMarca());
        assertEquals("Nuevo", entity.getNombre());
        assertEquals(EstadoEquipo.INOPERATIVO, entity.getEstado());
        assertTrue(entity.isRequiereMantenimiento());
    }
}
