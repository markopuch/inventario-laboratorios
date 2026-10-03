package com.utec.inventario.dto.response;
import java.util.List;
public record MovimientosReporteResponse(long total, List<ConteoReporteResponse> porTipo) {}

