package com.utec.inventario.domain;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

    private Integer id;
    private String userName;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
}
