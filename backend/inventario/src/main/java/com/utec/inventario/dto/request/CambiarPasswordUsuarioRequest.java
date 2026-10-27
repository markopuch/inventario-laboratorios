package com.utec.inventario.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.utec.inventario.security.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
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
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 15, maxLength = 72,
            description = "Mínimo 15 caracteres Unicode y máximo 72 bytes UTF-8. No se recorta; restablecer no revoca JWT emitidos.")
    private String password;

    @AssertTrue(message = PasswordPolicy.MESSAGE)
    @JsonIgnore
    public boolean isPasswordSegura() {
        return this.password == null || PasswordPolicy.isValid(this.password);
    }

    @JsonAnySetter
    public void rechazarCampoNoEditable(String nombreCampo, Object valor) {
        throw new IllegalArgumentException("El restablecimiento solo admite password.");
    }
}
