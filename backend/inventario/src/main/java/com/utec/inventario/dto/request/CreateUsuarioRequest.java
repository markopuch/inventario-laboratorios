package com.utec.inventario.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.utec.inventario.domain.RolUsuario;
import com.utec.inventario.security.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateUsuarioRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no puede superar los 50 caracteres")
    private String userName;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    @Size(max = 100, message = "El cargo no puede superar los 100 caracteres")
    private String cargo;

    @NotBlank(message = "La contraseña es obligatoria")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 15, maxLength = 72,
            description = "Mínimo 15 caracteres Unicode y máximo 72 bytes UTF-8. No se recorta; no puede contener solo blancos.")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    @NotNull(message = "El estado activo es obligatorio")
    @Builder.Default
    private Boolean activo = true;

    @AssertTrue(message = PasswordPolicy.MESSAGE)
    @JsonIgnore
    public boolean isPasswordSegura() {
        return this.password == null || PasswordPolicy.isValid(this.password);
    }

    @JsonAnySetter
    public void rechazarCampoNoEditable(String nombreCampo, Object valor) {
        throw new IllegalArgumentException("La creación de usuario solo admite los campos documentados.");
    }
}
