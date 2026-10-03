package com.utec.inventario.dto.response;

import com.utec.inventario.domain.EstadoEquipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EquipoMantenimientoResumenResponse {
    private Integer id;
    private String codigoInterno;
    private String nombre;
    private EstadoEquipo estado;
    private LaboratorioEquipoResponse laboratorio;
}
