package com.utec.inventario.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PasswordPolicyTest {

    static Stream<String> validPasswords() {
        return Stream.of("a".repeat(15), "a".repeat(72), "ñ".repeat(36), "😀".repeat(15),
                "😀".repeat(18), "  frase con espacios  ", "a".repeat(68) + "ññ");
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(null, "", "a".repeat(14), "😀".repeat(14), "a".repeat(73),
                "ñ".repeat(37), "😀".repeat(19), "a".repeat(69) + "ññ", " ".repeat(15),
                "\u00A0".repeat(15), "\uFEFF".repeat(15), "\u001c \t".repeat(15));
    }

    @ParameterizedTest(name = "Acepta caso válido {index}")
    @MethodSource("validPasswords")
    void aceptaFrasesUnicodeYLimitesEnBytesSinReglasDeComposicion(String password) {
        assertTrue(PasswordPolicy.isValid(password));
    }

    @ParameterizedTest(name = "Rechaza caso inválido {index}")
    @MethodSource("invalidPasswords")
    void rechazaMenosDe15CaracteresMasDe72BytesYClavesEnBlanco(String password) {
        assertFalse(PasswordPolicy.isValid(password));
    }
}
