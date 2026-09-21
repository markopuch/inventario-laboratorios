package com.utec.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateSedeRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
    private String direccion;

    @Size(max = 100, message = "El distrito no puede superar los 100 caracteres")
    private String distrito;

    @Size(max = 100, message = "El departamento no puede superar los 100 caracteres")
    private String departamento;
}
