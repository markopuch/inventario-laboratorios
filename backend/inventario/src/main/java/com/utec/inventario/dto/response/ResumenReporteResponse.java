package com.utec.inventario.dto.response;
import java.util.List;
public record ResumenReporteResponse(long totalEquipos, long totalMovimientos, long totalMantenimientos,
        List<ConteoReporteResponse> equiposPorEstado, List<ConteoReporteResponse> mantenimientosPorEstado) {}

