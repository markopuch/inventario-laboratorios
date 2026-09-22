package com.utec.inventario.dto.request;

import com.utec.inventario.domain.EstadoEquipo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateEquipoRequest {

    @NotBlank(message = "El código interno es obligatorio")
    @Size(max = 50, message = "El código interno no puede superar los 50 caracteres")
    private String codigoInterno;

    @NotNull(message = "El laboratorio es obligatorio")
    @Positive(message = "El ID de laboratorio debe ser mayor que cero")
    private Integer idLaboratorio;

    @Size(max = 100, message = "La serie UTEC no puede superar los 100 caracteres")
    private String serieUtec;

    @Size(max = 100, message = "El número de serie no puede superar los 100 caracteres")
    private String numeroSerie;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    private String marca;

    @Size(max = 100, message = "El modelo no puede superar los 100 caracteres")
    private String modelo;

    @NotNull(message = "El estado es obligatorio")
    private EstadoEquipo estado;

    @Min(value = 1900, message = "El año no puede ser menor que 1900")
    @Max(value = 2100, message = "El año no puede superar 2100")
    private Integer anio;

    @Size(max = 50, message = "La orden de compra no puede superar los 50 caracteres")
    private String ordenCompra;

    @Size(max = 200, message = "La ubicación interna no puede superar los 200 caracteres")
    private String ubicacionInterna;

    private String comentario;

    @NotNull(message = "Indica si el equipo requiere mantenimiento")
    private Boolean requiereMantenimiento;

    @NotNull(message = "La subcategoría es obligatoria")
    @Positive(message = "El ID de subcategoría debe ser mayor que cero")
    private Integer idSubcategoria;

    @Positive(message = "El ID de responsable debe ser mayor que cero")
    private Integer idResponsable;
}

