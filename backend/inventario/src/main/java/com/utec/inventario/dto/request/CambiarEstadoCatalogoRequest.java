package com.utec.inventario.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambiarEstadoCatalogoRequest {

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;
}
