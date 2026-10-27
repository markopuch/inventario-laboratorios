package com.utec.inventario.exception;

import com.utec.inventario.security.PasswordPolicy;

public class PasswordUsuarioInvalidaException extends RuntimeException {

    public PasswordUsuarioInvalidaException() {
        super(PasswordPolicy.MESSAGE);
    }
}
