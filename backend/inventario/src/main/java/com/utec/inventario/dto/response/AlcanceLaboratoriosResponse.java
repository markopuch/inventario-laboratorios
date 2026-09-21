package com.utec.inventario.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlcanceLaboratoriosResponse {

    private boolean alcanceGlobal;
    private List<LaboratorioAlcanceResponse> laboratorios;
}

