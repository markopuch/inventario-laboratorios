package com.utec.inventario.dto.response;

import java.time.OffsetDateTime;

import com.utec.inventario.domain.EstadoEquipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EquipoResponse {

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
    private SubcategoriaEquipoResponse subcategoria;
    private LaboratorioEquipoResponse laboratorio;
    private ResponsableEquipoResponse responsable;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}

