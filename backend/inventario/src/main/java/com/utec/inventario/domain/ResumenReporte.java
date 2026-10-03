package com.utec.inventario.domain;
import java.util.List;
public record ResumenReporte(long totalEquipos, long totalMovimientos, long totalMantenimientos,
        List<ConteoReporte> equiposPorEstado, List<ConteoReporte> mantenimientosPorEstado) {}

