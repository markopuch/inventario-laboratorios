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
public class MovimientoEquipo {

    private Integer id;
    private Equipo equipo;
    private Laboratorio laboratorioOrigen;
    private Laboratorio laboratorioDestino;
    private Usuario usuarioActor;
    private String tipoMovimiento;
    private String motivo;
    private OffsetDateTime fechaMovimiento;
}

