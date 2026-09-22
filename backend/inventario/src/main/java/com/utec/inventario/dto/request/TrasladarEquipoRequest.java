package com.utec.inventario.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.utec.inventario.exception.CampoTrasladoNoEditableException;

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
public class TrasladarEquipoRequest {

    @NotNull(message = "El laboratorio destino es obligatorio")
    @Positive(message = "El ID de laboratorio destino debe ser mayor que cero")
    private Integer idLaboratorioDestino;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;

    @Size(max = 200, message = "La ubicación interna destino no puede superar los 200 caracteres")
    private String ubicacionInternaDestino;

    @JsonAnySetter
    public void rechazarCampoNoEditable(String nombreCampo, Object valor) {
        throw new CampoTrasladoNoEditableException();
    }
}

