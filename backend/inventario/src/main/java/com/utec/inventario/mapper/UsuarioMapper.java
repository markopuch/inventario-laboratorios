package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.dto.request.CreateUsuarioRequest;
import com.utec.inventario.dto.request.UpdateUsuarioRequest;
import com.utec.inventario.dto.response.AdminUsuarioResponse;
import com.utec.inventario.dto.response.UsuarioResponse;
import com.utec.inventario.entity.UsuarioEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UsuarioMapper {

    @Mapping(source = "idUsuario", target = "id")
    @Mapping(source = "rol.nombre", target = "rol")
    Usuario convert(UsuarioEntity entity);

    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Usuario convert(CreateUsuarioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "rol", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Usuario convert(UpdateUsuarioRequest request);

    AdminUsuarioResponse toAdminResponse(Usuario usuario);

    List<AdminUsuarioResponse> toAdminResponse(List<Usuario> usuarios);

    List<Usuario> convert(List<UsuarioEntity> entities);
}
