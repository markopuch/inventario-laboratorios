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
public class MovimientoEquipoResponse {

    private Integer id;
    private String tipoMovimiento;
    private String motivo;
    private OffsetDateTime fechaMovimiento;
    private EquipoMovimientoResumenResponse equipo;
    private LaboratorioEquipoResponse laboratorioOrigen;
    private LaboratorioEquipoResponse laboratorioDestino;
    private UsuarioMovimientoResponse actor;
}

