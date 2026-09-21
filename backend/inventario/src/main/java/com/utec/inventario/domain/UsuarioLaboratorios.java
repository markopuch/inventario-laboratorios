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
public class UsuarioLaboratorios {

    private Usuario usuario;
    private List<Laboratorio> laboratorios;
}

