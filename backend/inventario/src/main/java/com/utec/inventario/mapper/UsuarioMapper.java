package com.utec.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.dto.response.UsuarioResponse;
import com.utec.inventario.entity.UsuarioEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UsuarioMapper {

    @Mapping(source = "idUsuario", target = "id")
    @Mapping(source = "rol.nombre", target = "rol")
    Usuario convert(UsuarioEntity entity);

    UsuarioResponse toResponse(Usuario usuario);
}
