package com.utec.inventario.exception;

public class CampoEquipoNoEditableException extends RuntimeException {

    public CampoEquipoNoEditableException() {
        super("El PUT solo admite los campos editables del equipo. El código interno es inmutable "
                + "y el cambio de laboratorio se realiza mediante el flujo de traslado.");
    }
}

