package com.utec.inventario.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.utec.inventario.domain.TipoMantenimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMantenimientoRequest {
    @NotNull(message = "El tipo es obligatorio")
    private TipoMantenimiento tipo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;

    @NotNull(message = "La fecha programada es obligatoria")
    private LocalDate fechaProgramada;

    @Positive(message = "El ID de responsable debe ser mayor que cero")
    private Integer idResponsable;

    @Size(max = 4000, message = "Las observaciones no pueden superar 4000 caracteres")
    private String observaciones;

    @JsonAnySetter
    public void rechazarCampoNoEditable(String campo, Object valor) {
        throw new IllegalArgumentException("El equipo, estado y fechas del servidor no son editables por PUT.");
    }
}
