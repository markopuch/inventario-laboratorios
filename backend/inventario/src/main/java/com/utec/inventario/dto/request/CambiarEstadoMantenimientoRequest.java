package com.utec.inventario.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.utec.inventario.domain.EstadoMantenimiento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambiarEstadoMantenimientoRequest {
    @NotNull(message = "El estado es obligatorio")
    private EstadoMantenimiento estado;

    @Size(max = 4000, message = "Las observaciones no pueden superar 4000 caracteres")
    private String observaciones;

    @JsonAnySetter
    public void rechazarCampoNoEditable(String campo, Object valor) {
        throw new IllegalArgumentException("Solo se admite el estado y observaciones en esta operación.");
    }
}
