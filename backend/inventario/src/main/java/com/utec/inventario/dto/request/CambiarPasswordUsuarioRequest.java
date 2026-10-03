package com.utec.inventario.dto.request;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambiarPasswordUsuarioRequest {

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 4, max = 72, message = "La contraseña debe tener entre 4 y 72 caracteres")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @AssertTrue(message = "La contraseña no puede superar los 72 bytes UTF-8")
    @JsonIgnore
    public boolean isPasswordDentroDelLimite() {
        return this.password == null || this.password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @JsonAnySetter
    public void rechazarCampoNoEditable(String nombreCampo, Object valor) {
        throw new IllegalArgumentException("El restablecimiento solo admite password.");
    }
}
