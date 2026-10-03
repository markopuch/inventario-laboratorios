package com.utec.inventario.domain;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FiltroAuditoria {
    private String entidad;
    private Integer idEntidad;
    private Integer idUsuario;
    private AccionAuditoria accion;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
}
