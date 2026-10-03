package com.utec.inventario.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoMantenimiento {
    PROGRAMADO, EN_PROCESO, COMPLETADO, CANCELADO;

    @JsonCreator
    public static EstadoMantenimiento desdeJson(String valor) {
        return valor == null ? null : EstadoMantenimiento.valueOf(valor);
    }
}
