package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.dto.request.CreateCategoriaRequest;
import com.utec.inventario.dto.request.UpdateCategoriaRequest;
import com.utec.inventario.dto.response.CategoriaResponse;
import com.utec.inventario.entity.CategoriaEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoriaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Categoria convert(CreateCategoriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Categoria convert(UpdateCategoriaRequest request);

    @Mapping(source = "idCategoria", target = "id")
    Categoria convert(CategoriaEntity entity);

    List<Categoria> convert(List<CategoriaEntity> entities);

    @Mapping(source = "id", target = "idCategoria")
    CategoriaEntity toEntity(Categoria categoria);

    CategoriaResponse toResponse(Categoria categoria);

    List<CategoriaResponse> toResponse(List<Categoria> categorias);

    // PUT reemplaza los campos editables, incluyendo una descripción nula.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "idCategoria", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    CategoriaEntity copy(@MappingTarget CategoriaEntity entity, Categoria categoria);
}
