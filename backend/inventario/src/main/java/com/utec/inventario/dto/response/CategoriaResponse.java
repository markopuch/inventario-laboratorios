package com.utec.inventario.dto.response;

import java.time.OffsetDateTime;

public record CategoriaResponse(
        Integer id,
        String nombre,
        String descripcion,
        boolean activo,
        OffsetDateTime fechaCreacion) {
}
