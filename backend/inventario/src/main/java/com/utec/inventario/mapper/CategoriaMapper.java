package com.utec.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
    Categoria toDomain(CreateCategoriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Categoria toDomain(UpdateCategoriaRequest request);

    @Mapping(source = "idCategoria", target = "id")
    Categoria toDomain(CategoriaEntity entity);

    @Mapping(source = "id", target = "idCategoria")
    CategoriaEntity toEntity(Categoria categoria);

    CategoriaResponse toResponse(Categoria categoria);
}
