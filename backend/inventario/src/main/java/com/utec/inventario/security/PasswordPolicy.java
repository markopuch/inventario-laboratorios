package com.utec.inventario.security;

import java.nio.charset.StandardCharsets;

/** Política para altas y restablecimientos; no se aplica al login de claves existentes. */
public final class PasswordPolicy {

    public static final int MIN_CHARACTERS = 15;
    public static final int MAX_UTF8_BYTES = 72;
    public static final String MESSAGE =
            "La contraseña debe tener al menos 15 caracteres y no superar los 72 bytes UTF-8.";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        if (password == null || password.codePointCount(0, password.length()) < MIN_CHARACTERS
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            return false;
        }
        // Misma definición de blanco que usuarios.js; no recorta ni altera la clave.
        return !password.codePoints().allMatch(character -> Character.isWhitespace(character)
                || Character.isSpaceChar(character) || character == 0xFEFF);
    }
}
