package com.utec.inventario.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoMantenimiento {
    PREVENTIVO, CORRECTIVO, CALIBRACION, OTRO;

    @JsonCreator
    public static TipoMantenimiento desdeJson(String valor) {
        return valor == null ? null : TipoMantenimiento.valueOf(valor);
    }
}
