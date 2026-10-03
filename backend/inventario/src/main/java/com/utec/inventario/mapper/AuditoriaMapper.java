package com.utec.inventario.mapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.utec.inventario.domain.Auditoria;
import com.utec.inventario.entity.AuditoriaEntity;
import com.utec.inventario.dto.response.AuditoriaResponse;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {
    com.utec.inventario.domain.FiltroAuditoria convert(com.utec.inventario.dto.request.FiltroAuditoriaRequest request);
    @Mapping(target = "id", source = "idAuditoria")
    @Mapping(target = "idUsuarioActor", source = "usuarioActor.idUsuario")
    Auditoria convert(AuditoriaEntity entity);
    List<Auditoria> convert(List<AuditoriaEntity> entities);
    AuditoriaResponse toResponse(Auditoria domain);
    List<AuditoriaResponse> toResponse(List<Auditoria> domains);
}
