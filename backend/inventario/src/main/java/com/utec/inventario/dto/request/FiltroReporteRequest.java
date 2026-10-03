package com.utec.inventario.dto.request;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.EstadoMantenimiento;
import com.utec.inventario.domain.TipoMantenimiento;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FiltroReporteRequest {
    @Positive private Integer idSede;
    @Positive private Integer idArea;
    @Positive private Integer idLaboratorio;
    private EstadoEquipo estado;
    private EstadoMantenimiento estadoMantenimiento;
    private TipoMantenimiento tipoMantenimiento;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fechaDesde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fechaHasta;
    @AssertTrue(message = "fechaHasta debe ser igual o posterior a fechaDesde")
    @JsonIgnore
    public boolean isRangoValido() {
        return fechaDesde == null || fechaHasta == null || !fechaHasta.isBefore(fechaDesde);
    }
}

