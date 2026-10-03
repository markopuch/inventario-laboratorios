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
    private String cargo;
    private String rol;
    private boolean activo;
    private OffsetDateTime fechaCreacion;

    // Conserva el constructor público utilizado por las proyecciones y contratos anteriores.
    public Usuario(Integer id, String userName, String nombre, String apellido, String email,
            String rol, boolean activo, OffsetDateTime fechaCreacion) {
        this(id, userName, nombre, apellido, email, null, rol, activo, fechaCreacion);
    }
}
