package com.utec.inventario.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActualizarLaboratoriosUsuarioRequest {

    @NotNull(message = "La lista de laboratorios es obligatoria")
    private List<
            @NotNull(message = "El ID de laboratorio es obligatorio")
            @Positive(message = "El ID de laboratorio debe ser mayor que cero")
            Integer> idsLaboratorio;
}

