package com.utec.inventario.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.utec.inventario.domain.RolUsuario;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambiarRolUsuarioRequest {

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    @JsonAnySetter
    public void rechazarCampoNoEditable(String nombreCampo, Object valor) {
        throw new IllegalArgumentException("El cambio de rol solo admite rol.");
    }
}
