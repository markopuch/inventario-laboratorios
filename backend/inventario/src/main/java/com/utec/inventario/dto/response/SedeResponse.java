package com.utec.inventario.dto.response;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SedeResponse {

    private Integer id;
    private String nombre;
    private String direccion;
    private String distrito;
    private String departamento;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
}
