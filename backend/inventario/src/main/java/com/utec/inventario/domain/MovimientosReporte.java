package com.utec.inventario.domain;
import java.util.List;
public record MovimientosReporte(long total, List<ConteoReporte> porTipo) {}

