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
public class UsuarioLaboratoriosResponse {

    private UsuarioResumenResponse usuario;
    private List<LaboratorioAlcanceResponse> laboratorios;
}

