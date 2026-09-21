package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.dto.request.CreateSubcategoriaRequest;
import com.utec.inventario.dto.request.UpdateSubcategoriaRequest;
import com.utec.inventario.dto.response.SubcategoriaResponse;
import com.utec.inventario.entity.CategoriaEntity;
import com.utec.inventario.entity.SubcategoriaEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubcategoriaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(source = "idCategoria", target = "categoria")
    Subcategoria convert(CreateSubcategoriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(source = "idCategoria", target = "categoria")
    Subcategoria convert(UpdateSubcategoriaRequest request);

    // El request identifica al padre; el Service comprueba y carga la relación JPA.
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idCategoria", target = "id")
    Categoria categoriaDesdeId(Integer idCategoria);

    @Mapping(source = "idSubcategoria", target = "id")
    Subcategoria convert(SubcategoriaEntity entity);

    @Mapping(source = "idCategoria", target = "id")
    Categoria convertCategoria(CategoriaEntity entity);

    List<Subcategoria> convert(List<SubcategoriaEntity> entities);

    @Mapping(source = "id", target = "idSubcategoria")
    @Mapping(target = "categoria", ignore = true)
    SubcategoriaEntity toEntity(Subcategoria subcategoria);

    SubcategoriaResponse toResponse(Subcategoria subcategoria);

    List<SubcategoriaResponse> toResponse(List<Subcategoria> subcategorias);

    // PUT reemplaza los campos editables; el Service asigna el padre validado.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "idSubcategoria", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    SubcategoriaEntity copy(@MappingTarget SubcategoriaEntity entity, Subcategoria subcategoria);
}
