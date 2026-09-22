package com.utec.inventario.domain;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Equipo {

    private Integer id;
    private String codigoInterno;
    private String serieUtec;
    private String numeroSerie;
    private String nombre;
    private String marca;
    private String modelo;
    private EstadoEquipo estado;
    private Integer anio;
    private String ordenCompra;
    private String ubicacionInterna;
    private String comentario;
    private boolean requiereMantenimiento;
    private Subcategoria subcategoria;
    private Laboratorio laboratorio;
    private Usuario responsable;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}

