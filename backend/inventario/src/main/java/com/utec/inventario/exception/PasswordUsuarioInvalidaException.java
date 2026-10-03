package com.utec.inventario.exception;

public class PasswordUsuarioInvalidaException extends RuntimeException {

    public PasswordUsuarioInvalidaException() {
        super("La contraseña debe tener entre 4 y 72 caracteres y no superar los 72 bytes UTF-8.");
    }
}
