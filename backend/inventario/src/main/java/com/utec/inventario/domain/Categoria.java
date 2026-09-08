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
public class Categoria {

    private Integer id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
}
