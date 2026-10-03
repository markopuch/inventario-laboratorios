package com.utec.inventario.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.utec.inventario.domain.EstadoMantenimiento;
import com.utec.inventario.domain.TipoMantenimiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MantenimientoResponse {
    private Integer id;
    private EquipoMantenimientoResumenResponse equipo;
    private TipoMantenimiento tipo;
    private String descripcion;
    private LocalDate fechaProgramada;
    private OffsetDateTime fechaInicio;
    private OffsetDateTime fechaFin;
    private ResponsableEquipoResponse responsable;
    private EstadoMantenimiento estado;
    private String observaciones;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
