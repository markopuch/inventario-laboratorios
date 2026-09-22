package com.utec.inventario.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrasladoEquipo {

    private Equipo equipo;
    private MovimientoEquipo movimiento;
}

