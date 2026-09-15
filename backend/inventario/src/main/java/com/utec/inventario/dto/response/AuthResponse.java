package com.utec.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    @ToString.Exclude
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UsuarioResponse usuario;
}
