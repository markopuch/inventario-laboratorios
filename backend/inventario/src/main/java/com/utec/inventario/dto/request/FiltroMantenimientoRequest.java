package com.utec.inventario.dto.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.utec.inventario.domain.EstadoMantenimiento;
import com.utec.inventario.domain.TipoMantenimiento;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FiltroMantenimientoRequest {
    @Positive(message = "El ID de equipo debe ser mayor que cero")
    private Integer idEquipo;
    @Positive(message = "El ID de laboratorio debe ser mayor que cero")
    private Integer idLaboratorio;
    private EstadoMantenimiento estado;
    private TipoMantenimiento tipo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDesde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaHasta;

    @JsonIgnore
    @AssertTrue(message = "La fecha hasta no puede ser anterior a la fecha desde")
    public boolean isRangoFechasValido() {
        return fechaDesde == null || fechaHasta == null || !fechaHasta.isBefore(fechaDesde);
    }
}
