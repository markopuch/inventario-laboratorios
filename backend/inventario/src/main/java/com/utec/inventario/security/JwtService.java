package com.utec.inventario.security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String ISSUER = "inventario-laboratorios";

    private final SecretKey signKey;
    private final JwtParser parser;
    private final long expirationSeconds;

    @Autowired
    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("La duración del JWT debe ser positiva.");
        }
        try {
            this.signKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("JWT_SECRET debe contener al menos 32 bytes aleatorios en Base64.");
        }
        this.expirationSeconds = expirationSeconds;
        this.parser = Jwts.parser()
                .verifyWith(this.signKey)
                .requireIssuer(ISSUER)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .build();
    }

    public String generateToken(UserInfoDetails usuario) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(usuario.getUsername())
                .claim("id", usuario.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(this.expirationSeconds)))
                .signWith(this.signKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return this.extractAllClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserInfoDetails usuario) {
        Claims claims = this.extractAllClaims(token);
        return usuario.isEnabled()
                && usuario.getUsername().equals(claims.getSubject())
                && usuario.getId().equals(claims.get("id", Integer.class));
    }

    public long getExpirationSeconds() {
        return this.expirationSeconds;
    }

    private Claims extractAllClaims(String token) {
        Claims claims = this.parser.parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null || claims.getIssuedAt() == null
                || claims.get("id", Integer.class) == null) {
            throw new JwtException("Token incompleto.");
        }
        return claims;
    }
}
