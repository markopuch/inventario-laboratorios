package com.utec.inventario.domain;
import java.time.OffsetDateTime;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Auditoria {
    private Integer id;
    private Integer idUsuarioActor;
    private String userNameActor;
    private AccionAuditoria accion;
    private String entidad;
    private Integer idEntidad;
    private String cambios;
    private OffsetDateTime fecha;
}

