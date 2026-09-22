package com.utec.inventario.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoEquipo {
    OPERATIVO,
    MANTENIMIENTO,
    INOPERATIVO,
    BAJA;

    // Solo nombres del CHECK de V3: tampoco se aceptan ordinales numéricos en JSON.
    @JsonCreator
    public static EstadoEquipo desdeJson(String valor) {
        return valor == null ? null : EstadoEquipo.valueOf(valor);
    }
}

