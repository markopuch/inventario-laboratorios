package com.utec.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LaboratorioEquipoResponse {

    private Integer id;
    private String codigo;
    private String nombre;
}

