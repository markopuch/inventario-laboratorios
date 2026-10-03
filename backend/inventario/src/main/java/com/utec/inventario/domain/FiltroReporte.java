package com.utec.inventario.domain;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FiltroReporte {
    private Integer idSede;
    private Integer idArea;
    private Integer idLaboratorio;
    private EstadoEquipo estado;
    private EstadoMantenimiento estadoMantenimiento;
    private TipoMantenimiento tipoMantenimiento;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
}
