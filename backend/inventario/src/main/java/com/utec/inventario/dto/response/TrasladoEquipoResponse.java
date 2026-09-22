package com.utec.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrasladoEquipoResponse {

    private EquipoResponse equipo;
    private MovimientoEquipoResponse movimiento;
}

