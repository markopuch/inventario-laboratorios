package com.utec.inventario.domain;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    private Integer id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
}
