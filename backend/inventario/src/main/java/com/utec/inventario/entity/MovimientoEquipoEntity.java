package com.utec.inventario.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movimiento_equipo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoEquipoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_equipo", nullable = false, updatable = false)
    private EquipoEntity equipo;

    // V3 permite NULL en el origen de registros históricos.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_laboratorio_origen", updatable = false)
    private LaboratorioEntity laboratorioOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_laboratorio_destino", nullable = false, updatable = false)
    private LaboratorioEntity laboratorioDestino;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_actor", nullable = false, updatable = false)
    private UsuarioEntity usuarioActor;

    // El CHECK no define un enum cerrado: las lecturas conservan tipos históricos válidos.
    @Column(name = "tipo_movimiento", nullable = false, length = 30, updatable = false)
    private String tipoMovimiento;

    @Column(name = "motivo", nullable = false, length = 500, updatable = false)
    private String motivo;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_movimiento", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaMovimiento;
}

