package com.utec.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioResumenResponse {

    private Integer id;
    private String userName;
    private String nombre;
    private String apellido;
    private String rol;
}

