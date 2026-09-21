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
public class LaboratorioResponse {

    private Integer id;
    private String nombre;
    private String codigo;
    private String ubicacion;
    private boolean activo;
    private OffsetDateTime fechaCreacion;
    private AreaResumenResponse area;
}
