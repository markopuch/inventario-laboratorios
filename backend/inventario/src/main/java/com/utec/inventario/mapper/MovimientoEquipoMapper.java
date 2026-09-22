package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.MovimientoEquipo;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.dto.response.EquipoMovimientoResumenResponse;
import com.utec.inventario.dto.response.LaboratorioEquipoResponse;
import com.utec.inventario.dto.response.MovimientoEquipoResponse;
import com.utec.inventario.dto.response.UsuarioMovimientoResponse;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.MovimientoEquipoEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MovimientoEquipoMapper {

    @Mapping(source = "idMovimiento", target = "id")
    @Mapping(target = "usuarioActor", ignore = true)
    MovimientoEquipo convert(MovimientoEquipoEntity entity);

    // El Service proporciona la proyección pública; nunca se recorre usuarioActor JPA.
    default MovimientoEquipo convert(MovimientoEquipoEntity entity, Usuario actor) {
        MovimientoEquipo movimiento = this.convert(entity);
        if (movimiento != null) {
            movimiento.setUsuarioActor(actor);
        }
        return movimiento;
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idEquipo", target = "id")
    @Mapping(source = "codigoInterno", target = "codigoInterno")
    @Mapping(source = "nombre", target = "nombre")
    Equipo convertEquipoResumen(EquipoEntity entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idLaboratorio", target = "id")
    @Mapping(source = "codigo", target = "codigo")
    @Mapping(source = "nombre", target = "nombre")
    Laboratorio convertLaboratorioResumen(LaboratorioEntity entity);

    List<MovimientoEquipo> convert(List<MovimientoEquipoEntity> entities);

    @Mapping(source = "usuarioActor", target = "actor")
    MovimientoEquipoResponse toResponse(MovimientoEquipo movimiento);

    List<MovimientoEquipoResponse> toResponse(List<MovimientoEquipo> movimientos);

    EquipoMovimientoResumenResponse toEquipoResumen(Equipo equipo);

    LaboratorioEquipoResponse toLaboratorioResumen(Laboratorio laboratorio);

    UsuarioMovimientoResponse toActorResponse(Usuario actor);
}

