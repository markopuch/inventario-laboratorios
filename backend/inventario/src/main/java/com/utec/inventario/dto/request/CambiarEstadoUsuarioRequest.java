package com.utec.inventario.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambiarEstadoUsuarioRequest {

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;

    @JsonAnySetter
    public void rechazarCampoNoEditable(String nombreCampo, Object valor) {
        throw new IllegalArgumentException("El cambio de estado solo admite activo.");
    }
}
