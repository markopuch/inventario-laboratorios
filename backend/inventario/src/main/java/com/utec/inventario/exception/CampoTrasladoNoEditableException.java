package com.utec.inventario.exception;

public class CampoTrasladoNoEditableException extends RuntimeException {

    public CampoTrasladoNoEditableException() {
        super("El traslado solo admite idLaboratorioDestino, motivo y ubicacionInternaDestino. "
                + "El equipo, origen, actor, tipo y fecha los determina el servidor.");
    }
}

