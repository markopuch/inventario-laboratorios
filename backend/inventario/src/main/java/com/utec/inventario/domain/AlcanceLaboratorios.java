package com.utec.inventario.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlcanceLaboratorios {

    private boolean alcanceGlobal;
    private List<Laboratorio> laboratorios;
}

