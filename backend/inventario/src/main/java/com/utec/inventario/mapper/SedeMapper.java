package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Sede;
import com.utec.inventario.dto.request.CreateSedeRequest;
import com.utec.inventario.dto.request.UpdateSedeRequest;
import com.utec.inventario.dto.response.SedeResponse;
import com.utec.inventario.entity.SedeEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SedeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Sede convert(CreateSedeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Sede convert(UpdateSedeRequest request);

    @Mapping(source = "idSede", target = "id")
    Sede convert(SedeEntity entity);

    List<Sede> convert(List<SedeEntity> entities);

    @Mapping(source = "id", target = "idSede")
    SedeEntity toEntity(Sede sede);

    SedeResponse toResponse(Sede sede);

    List<SedeResponse> toResponse(List<Sede> sedes);

    // PUT sustituye los campos editables y conserva los administrados por el servidor.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "idSede", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    SedeEntity copy(@MappingTarget SedeEntity entity, Sede sede);
}
