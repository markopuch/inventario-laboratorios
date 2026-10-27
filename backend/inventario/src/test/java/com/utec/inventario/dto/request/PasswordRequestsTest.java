package com.utec.inventario.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.utec.inventario.domain.RolUsuario;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import tools.jackson.databind.ObjectMapper;

class PasswordRequestsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    static Stream<String> validPasswords() {
        return Stream.of("a".repeat(15), "a".repeat(72), "ñ".repeat(36), "😀".repeat(15),
                "😀".repeat(18), "  frase con espacios  ");
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(null, "", "a".repeat(14), "😀".repeat(14), "a".repeat(73),
                "ñ".repeat(37), "😀".repeat(19), " ".repeat(15), "\u00A0".repeat(15),
                "\uFEFF".repeat(15), "\u001c \t".repeat(15));
    }

    @ParameterizedTest(name = "DTO acepta caso {index}")
    @MethodSource("validPasswords")
    void crearYRestablecerCompartenLaPoliticaSinAlterarLaClave(String password) {
        CreateUsuarioRequest create = createRequest(password);
        CambiarPasswordUsuarioRequest reset = new CambiarPasswordUsuarioRequest(password);
        assertTrue(validator.validate(create).isEmpty());
        assertTrue(validator.validate(reset).isEmpty());
        assertEquals(password, create.getPassword());
        assertEquals(password, reset.getPassword());
    }

    @ParameterizedTest(name = "DTO rechaza caso {index}")
    @MethodSource("invalidPasswords")
    void crearYRestablecerRechazanClavesFueraDeLaPolitica(String password) {
        assertFalse(validator.validate(createRequest(password)).isEmpty());
        assertFalse(validator.validate(new CambiarPasswordUsuarioRequest(password)).isEmpty());
    }

    @Test
    void loginConservaLasClavesCortasDeCuentasExistentes() {
        assertTrue(validator.validate(AuthRequest.builder().userName("legacy").password("1234").build()).isEmpty());
    }

    @Test
    void dtoNoExponeClaveNiValidacionDerivadaEnJsonOToString() {
        String password = "dato privado de prueba";
        CreateUsuarioRequest create = createRequest(password);
        CambiarPasswordUsuarioRequest reset = new CambiarPasswordUsuarioRequest(password);
        ObjectMapper mapper = new ObjectMapper();
        for (Object request : new Object[] {create, reset}) {
            String json = mapper.writeValueAsString(request);
            assertFalse(json.contains(password));
            assertFalse(json.contains("password"));
            assertFalse(request.toString().contains(password));
        }
    }

    private static CreateUsuarioRequest createRequest(String password) {
        return CreateUsuarioRequest.builder().userName("prueba").nombre("Usuario").apellido("Prueba")
                .email("prueba@inventario.test").password(password).rol(RolUsuario.LECTOR).activo(true).build();
    }
}
