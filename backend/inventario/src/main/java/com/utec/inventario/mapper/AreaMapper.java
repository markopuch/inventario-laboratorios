package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Area;
import com.utec.inventario.domain.Sede;
import com.utec.inventario.dto.request.CreateAreaRequest;
import com.utec.inventario.dto.request.UpdateAreaRequest;
import com.utec.inventario.dto.response.AreaResponse;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.SedeEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AreaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(source = "idSede", target = "sede")
    Area convert(CreateAreaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(source = "idSede", target = "sede")
    Area convert(UpdateAreaRequest request);

    // El Service comprueba el ID recibido y asigna la Entity padre validada.
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idSede", target = "id")
    Sede sedeDesdeId(Integer idSede);

    @Mapping(source = "idArea", target = "id")
    Area convert(AreaEntity entity);

    @Mapping(source = "idSede", target = "id")
    Sede convertSede(SedeEntity entity);

    List<Area> convert(List<AreaEntity> entities);

    @Mapping(source = "id", target = "idArea")
    @Mapping(target = "sede", ignore = true)
    AreaEntity toEntity(Area area);

    AreaResponse toResponse(Area area);

    List<AreaResponse> toResponse(List<Area> areas);

    // PUT sustituye los campos editables y conserva los administrados por el servidor.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "idArea", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "sede", ignore = true)
    AreaEntity copy(@MappingTarget AreaEntity entity, Area area);
}
