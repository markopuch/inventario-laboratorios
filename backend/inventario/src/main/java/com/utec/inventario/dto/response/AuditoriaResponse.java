package com.utec.inventario.dto.response;
import java.time.OffsetDateTime;
import com.utec.inventario.domain.AccionAuditoria;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditoriaResponse {
    private Integer id;
    private Integer idUsuarioActor;
    private String userNameActor;
    private AccionAuditoria accion;
    private String entidad;
    private Integer idEntidad;
    private String cambios;
    private OffsetDateTime fecha;
}

