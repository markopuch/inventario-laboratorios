package com.utec.inventario.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private SecretKey key;
    private JwtService jwtService;
    private UserInfoDetails usuario;

    @BeforeEach
    void preparar() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        this.key = Keys.hmacShaKeyFor(bytes);
        this.jwtService = new JwtService(Base64.getEncoder().encodeToString(bytes), 300);
        this.usuario = mock(UserInfoDetails.class);
        when(this.usuario.getId()).thenReturn(7);
        when(this.usuario.getUsername()).thenReturn("marko");
        when(this.usuario.isEnabled()).thenReturn(true);
    }

    @Test
    void emiteTokenFirmadoConCaducidadSinContrasenaNiAutoridades() {
        String token = this.jwtService.generateToken(this.usuario);
        var claims = Jwts.parser().verifyWith(this.key).build().parseSignedClaims(token).getPayload();
        assertAll(
                () -> assertEquals("marko", this.jwtService.extractUsername(token)),
                () -> assertTrue(this.jwtService.validateToken(token, this.usuario)),
                () -> assertEquals(7, claims.get("id", Integer.class)),
                () -> assertEquals("inventario-laboratorios", claims.getIssuer()),
                () -> assertEquals(300_000, claims.getExpiration().getTime() - claims.getIssuedAt().getTime()),
                () -> assertEquals(300, this.jwtService.getExpirationSeconds()),
                () -> assertEquals(5, claims.size()),
                () -> assertFalse(claims.containsKey("password")),
                () -> assertFalse(claims.containsKey("passwordHash")),
                () -> assertFalse(claims.containsKey("roles")));
    }

    @Test
    void rechazaLaAlteracionDelSujetoSinFirmarDeNuevo() {
        String token = this.jwtService.generateToken(this.usuario);
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("marko", "romel");
        String altered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        assertThrows(JwtException.class, () -> this.jwtService.extractUsername(altered));
    }

    @Test
    void rechazaUnTokenVencidoAunqueLaFirmaSeaCorrecta() {
        String token = tokenValido()
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(this.key, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> this.jwtService.validateToken(token, this.usuario));
    }

    @ParameterizedTest
    @ValueSource(strings = { "sub", "exp", "iat", "id", "iss" })
    void rechazaLosTokensSinLosDatosObligatorios(String claim) {
        String token = tokenValido().claims().delete(claim).and()
                .signWith(this.key, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> this.jwtService.extractUsername(token));
    }

    @Test
    void rechazaUnEmisorDistinto() {
        String token = tokenValido().issuer("otra-aplicacion")
                .signWith(this.key, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> this.jwtService.extractUsername(token));
    }

    @Test
    void compruebaElEstadoYLaIdentidadActualDelUsuario() {
        String token = this.jwtService.generateToken(this.usuario);
        when(this.usuario.isEnabled()).thenReturn(false);
        assertFalse(this.jwtService.validateToken(token, this.usuario));
        when(this.usuario.isEnabled()).thenReturn(true);
        when(this.usuario.getId()).thenReturn(99);
        assertFalse(this.jwtService.validateToken(token, this.usuario));
        when(this.usuario.getId()).thenReturn(7);
        when(this.usuario.getUsername()).thenReturn("romel");
        assertFalse(this.jwtService.validateToken(token, this.usuario));
    }

    private JwtBuilder tokenValido() {
        Instant now = Instant.now();
        return Jwts.builder().issuer("inventario-laboratorios").subject("marko").claim("id", 7)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(300)));
    }
}
