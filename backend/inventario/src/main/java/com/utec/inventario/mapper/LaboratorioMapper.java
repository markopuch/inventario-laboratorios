package com.utec.inventario.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Area;
import com.utec.inventario.domain.Sede;
import com.utec.inventario.dto.request.CreateLaboratorioRequest;
import com.utec.inventario.dto.request.UpdateLaboratorioRequest;
import com.utec.inventario.dto.response.LaboratorioResponse;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.SedeEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LaboratorioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(source = "idArea", target = "area")
    Laboratorio convert(CreateLaboratorioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(source = "idArea", target = "area")
    Laboratorio convert(UpdateLaboratorioRequest request);

    // El Service comprueba el ID recibido y asigna la Entity padre validada.
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idArea", target = "id")
    Area areaDesdeId(Integer idArea);

    @Mapping(source = "idLaboratorio", target = "id")
    Laboratorio convert(LaboratorioEntity entity);

    @Mapping(source = "idArea", target = "id")
    Area convertArea(AreaEntity entity);

    @Mapping(source = "idSede", target = "id")
    Sede convertSede(SedeEntity entity);

    List<Laboratorio> convert(List<LaboratorioEntity> entities);

    @Mapping(source = "id", target = "idLaboratorio")
    @Mapping(target = "area", ignore = true)
    LaboratorioEntity toEntity(Laboratorio laboratorio);

    LaboratorioResponse toResponse(Laboratorio laboratorio);

    List<LaboratorioResponse> toResponse(List<Laboratorio> laboratorios);

    // PUT sustituye los campos editables y conserva los administrados por el servidor.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "idLaboratorio", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "area", ignore = true)
    LaboratorioEntity copy(@MappingTarget LaboratorioEntity entity, Laboratorio laboratorio);
}
