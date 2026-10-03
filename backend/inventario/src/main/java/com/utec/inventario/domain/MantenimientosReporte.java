package com.utec.inventario.domain;
import java.util.List;
public record MantenimientosReporte(long total, List<ConteoReporte> porEstado, List<ConteoReporte> porTipo) {}

