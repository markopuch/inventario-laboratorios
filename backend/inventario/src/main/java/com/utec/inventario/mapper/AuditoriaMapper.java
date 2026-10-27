package com.utec.inventario.mapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.utec.inventario.domain.Auditoria;
import com.utec.inventario.domain.Pagina;
import com.utec.inventario.entity.AuditoriaEntity;
import com.utec.inventario.dto.response.AuditoriaResponse;
import com.utec.inventario.dto.response.PaginaAuditoriaResponse;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {
    com.utec.inventario.domain.FiltroAuditoria convert(com.utec.inventario.dto.request.FiltroAuditoriaRequest request);
    @Mapping(target = "id", source = "idAuditoria")
    @Mapping(target = "idUsuarioActor", source = "usuarioActor.idUsuario")
    Auditoria convert(AuditoriaEntity entity);
    List<Auditoria> convert(List<AuditoriaEntity> entities);
    AuditoriaResponse toResponse(Auditoria domain);
    List<AuditoriaResponse> toResponse(List<Auditoria> domains);

    default PaginaAuditoriaResponse toResponse(Pagina<Auditoria> pagina) {
        return new PaginaAuditoriaResponse(this.toResponse(pagina.content()), pagina.number(), pagina.size(),
                pagina.totalElements(), pagina.totalPages(), pagina.first(), pagina.last());
    }
}
