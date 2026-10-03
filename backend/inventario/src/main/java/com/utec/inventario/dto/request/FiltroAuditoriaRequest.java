package com.utec.inventario.dto.request;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.utec.inventario.domain.AccionAuditoria;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FiltroAuditoriaRequest {
    @Size(max = 40) private String entidad;
    @Positive private Integer idEntidad;
    @Positive private Integer idUsuario;
    private AccionAuditoria accion;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fechaDesde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fechaHasta;
    @AssertTrue(message = "fechaHasta debe ser igual o posterior a fechaDesde")
    @JsonIgnore
    public boolean isRangoValido() {
        return fechaDesde == null || fechaHasta == null || !fechaHasta.isBefore(fechaDesde);
    }
}

