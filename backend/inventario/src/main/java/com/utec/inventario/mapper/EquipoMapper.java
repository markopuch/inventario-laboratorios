package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.dto.request.CreateEquipoRequest;
import com.utec.inventario.dto.request.UpdateEquipoRequest;
import com.utec.inventario.dto.response.EquipoResponse;
import com.utec.inventario.dto.response.LaboratorioEquipoResponse;
import com.utec.inventario.dto.response.ResponsableEquipoResponse;
import com.utec.inventario.dto.response.SubcategoriaEquipoResponse;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.SubcategoriaEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EquipoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(source = "idSubcategoria", target = "subcategoria")
    @Mapping(source = "idLaboratorio", target = "laboratorio")
    @Mapping(source = "idResponsable", target = "responsable")
    Equipo convert(CreateEquipoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codigoInterno", ignore = true)
    @Mapping(target = "laboratorio", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(source = "idSubcategoria", target = "subcategoria")
    @Mapping(source = "idResponsable", target = "responsable")
    Equipo convert(UpdateEquipoRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idSubcategoria", target = "id")
    Subcategoria subcategoriaDesdeId(Integer idSubcategoria);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idLaboratorio", target = "id")
    Laboratorio laboratorioDesdeId(Integer idLaboratorio);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idResponsable", target = "id")
    Usuario responsableDesdeId(Integer idResponsable);

    // El Service añade el responsable obtenido mediante una proyección pública.
    @Mapping(source = "idEquipo", target = "id")
    @Mapping(target = "responsable", ignore = true)
    Equipo convert(EquipoEntity entity);

    default Equipo convert(EquipoEntity entity, Usuario responsable) {
        Equipo equipo = this.convert(entity);
        if (equipo != null) {
            equipo.setResponsable(responsable);
        }
        return equipo;
    }

    @Mapping(source = "idSubcategoria", target = "id")
    @Mapping(target = "categoria", ignore = true)
    Subcategoria convertSubcategoria(SubcategoriaEntity entity);

    @Mapping(source = "idLaboratorio", target = "id")
    @Mapping(target = "area", ignore = true)
    Laboratorio convertLaboratorio(LaboratorioEntity entity);

    List<Equipo> convert(List<EquipoEntity> entities);

    @Mapping(source = "id", target = "idEquipo")
    @Mapping(target = "subcategoria", ignore = true)
    @Mapping(target = "laboratorio", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    EquipoEntity toEntity(Equipo equipo);

    EquipoResponse toResponse(Equipo equipo);

    List<EquipoResponse> toResponse(List<Equipo> equipos);

    SubcategoriaEquipoResponse toSubcategoriaResponse(Subcategoria subcategoria);

    LaboratorioEquipoResponse toLaboratorioResponse(Laboratorio laboratorio);

    ResponsableEquipoResponse toResponsableResponse(Usuario responsable);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "idEquipo", ignore = true)
    @Mapping(target = "codigoInterno", ignore = true)
    @Mapping(target = "laboratorio", ignore = true)
    @Mapping(target = "subcategoria", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    EquipoEntity copy(@MappingTarget EquipoEntity entity, Equipo equipo);
}

