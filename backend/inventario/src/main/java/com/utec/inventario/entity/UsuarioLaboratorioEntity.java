package com.utec.inventario.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_laboratorio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioLaboratorioEntity {

    @EmbeddedId
    private UsuarioLaboratorioId id;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @MapsId("idLaboratorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_laboratorio", nullable = false)
    private LaboratorioEntity laboratorio;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    // Conserva la fecha original también al desactivar y reactivar la relación.
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_asignacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaAsignacion;
}

