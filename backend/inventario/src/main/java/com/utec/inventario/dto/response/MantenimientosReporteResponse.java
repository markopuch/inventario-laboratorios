package com.utec.inventario.dto.response;
import java.util.List;
public record MantenimientosReporteResponse(long total, List<ConteoReporteResponse> porEstado,
        List<ConteoReporteResponse> porTipo) {}

