package com.utec.inventario.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.utec.inventario.domain.EstadoEquipo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "equipo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Integer idEquipo;

    @Column(name = "codigo_interno", nullable = false, unique = true, length = 50, updatable = false)
    private String codigoInterno;

    @Column(name = "serie_utec", unique = true, length = 100)
    private String serieUtec;

    @Column(name = "numero_serie", unique = true, length = 100)
    private String numeroSerie;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "marca", length = 100)
    private String marca;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoEquipo estado;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "orden_compra", length = 50)
    private String ordenCompra;

    @Column(name = "ubicacion_interna", length = 200)
    private String ubicacionInterna;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "requiere_mantenimiento", nullable = false)
    private boolean requiereMantenimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_subcategoria", nullable = false)
    private SubcategoriaEntity subcategoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_laboratorio", nullable = false, updatable = false)
    private LaboratorioEntity laboratorio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable")
    private UsuarioEntity responsable;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    // PostgreSQL genera el valor inicial; el Service lo actualiza en PUT y baja lógica.
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_actualizacion", nullable = false, insertable = false)
    private OffsetDateTime fechaActualizacion;
}

