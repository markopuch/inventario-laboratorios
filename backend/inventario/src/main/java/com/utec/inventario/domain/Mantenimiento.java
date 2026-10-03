package com.utec.inventario.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Mantenimiento {
    private Integer id;
    private Equipo equipo;
    private TipoMantenimiento tipo;
    private String descripcion;
    private LocalDate fechaProgramada;
    private OffsetDateTime fechaInicio;
    private OffsetDateTime fechaFin;
    private Usuario responsable;
    private EstadoMantenimiento estado;
    private String observaciones;
    private EstadoEquipo estadoEquipoAnterior;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
