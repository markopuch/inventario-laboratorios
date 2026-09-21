package com.utec.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.domain.UsuarioLaboratorio;
import com.utec.inventario.domain.UsuarioLaboratorios;
import com.utec.inventario.dto.response.AlcanceLaboratoriosResponse;
import com.utec.inventario.dto.response.LaboratorioAlcanceResponse;
import com.utec.inventario.dto.response.UsuarioLaboratoriosResponse;
import com.utec.inventario.dto.response.UsuarioResumenResponse;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UsuarioLaboratorioMapper {

    // El usuario público viene de una proyección: no se recorre entity.usuario.
    @Mapping(source = "usuario", target = "usuario")
    @Mapping(source = "entity.laboratorio", target = "laboratorio")
    @Mapping(source = "entity.activo", target = "activo")
    @Mapping(source = "entity.fechaAsignacion", target = "fechaAsignacion")
    UsuarioLaboratorio convert(UsuarioLaboratorioEntity entity, Usuario usuario);

    // La asignación solo necesita los datos propios del laboratorio.
    @Mapping(source = "idLaboratorio", target = "id")
    @Mapping(target = "area", ignore = true)
    Laboratorio convertLaboratorio(LaboratorioEntity entity);

    UsuarioResumenResponse toUsuarioResumen(Usuario usuario);

    LaboratorioAlcanceResponse toLaboratorioAlcance(Laboratorio laboratorio);

    UsuarioLaboratoriosResponse toResponse(UsuarioLaboratorios asignaciones);

    AlcanceLaboratoriosResponse toResponse(AlcanceLaboratorios alcance);
}

