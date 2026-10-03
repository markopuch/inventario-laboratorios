package com.utec.inventario.entity;

import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import com.utec.inventario.domain.AccionAuditoria;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auditoria")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditoriaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_actor", updatable = false)
    private UsuarioEntity usuarioActor;
    @Column(name = "username_actor", nullable = false, length = 50, updatable = false)
    private String userNameActor;
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 40, updatable = false)
    private AccionAuditoria accion;
    @Column(name = "entidad", nullable = false, length = 40, updatable = false)
    private String entidad;
    @Column(name = "id_entidad", nullable = false, updatable = false)
    private Integer idEntidad;
    @Column(name = "cambios", nullable = false, length = 2000, updatable = false)
    private String cambios;
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fecha;
}

