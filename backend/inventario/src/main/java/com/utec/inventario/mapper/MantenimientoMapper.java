package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Mantenimiento;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.dto.request.CreateMantenimientoRequest;
import com.utec.inventario.dto.request.UpdateMantenimientoRequest;
import com.utec.inventario.dto.response.EquipoMantenimientoResumenResponse;
import com.utec.inventario.dto.response.LaboratorioEquipoResponse;
import com.utec.inventario.dto.response.MantenimientoResponse;
import com.utec.inventario.dto.response.ResponsableEquipoResponse;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.MantenimientoEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MantenimientoMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idEquipo", target = "equipo")
    @Mapping(source = "tipo", target = "tipo")
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "fechaProgramada", target = "fechaProgramada")
    @Mapping(source = "idResponsable", target = "responsable")
    @Mapping(source = "observaciones", target = "observaciones")
    Mantenimiento convert(CreateMantenimientoRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "tipo", target = "tipo")
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "fechaProgramada", target = "fechaProgramada")
    @Mapping(source = "idResponsable", target = "responsable")
    @Mapping(source = "observaciones", target = "observaciones")
    Mantenimiento convert(UpdateMantenimientoRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idEquipo", target = "id")
    Equipo equipoDesdeId(Integer idEquipo);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idResponsable", target = "id")
    Usuario responsableDesdeId(Integer idResponsable);

    @Mapping(source = "idMantenimiento", target = "id")
    @Mapping(target = "responsable", ignore = true)
    Mantenimiento convert(MantenimientoEntity entity);

    default Mantenimiento convert(MantenimientoEntity entity, Usuario responsable) {
        Mantenimiento result = convert(entity);
        if (result != null) result.setResponsable(responsable);
        return result;
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idEquipo", target = "id")
    @Mapping(source = "codigoInterno", target = "codigoInterno")
    @Mapping(source = "nombre", target = "nombre")
    @Mapping(source = "estado", target = "estado")
    @Mapping(source = "laboratorio", target = "laboratorio")
    Equipo convertEquipo(EquipoEntity entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idLaboratorio", target = "id")
    @Mapping(source = "codigo", target = "codigo")
    @Mapping(source = "nombre", target = "nombre")
    Laboratorio convertLaboratorio(LaboratorioEntity entity);

    @Mapping(source = "id", target = "idMantenimiento")
    @Mapping(target = "equipo", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    MantenimientoEntity toEntity(Mantenimiento mantenimiento);

    MantenimientoResponse toResponse(Mantenimiento mantenimiento);
    List<MantenimientoResponse> toResponse(List<Mantenimiento> mantenimientos);
    EquipoMantenimientoResumenResponse toEquipoResponse(Equipo equipo);
    LaboratorioEquipoResponse toLaboratorioResponse(Laboratorio laboratorio);
    ResponsableEquipoResponse toResponsableResponse(Usuario responsable);
}
