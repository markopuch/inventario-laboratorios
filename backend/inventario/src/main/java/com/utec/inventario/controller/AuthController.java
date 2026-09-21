package com.utec.inventario.controller;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.dto.request.AuthRequest;
import com.utec.inventario.dto.response.AuthResponse;
import com.utec.inventario.dto.response.AlcanceLaboratoriosResponse;
import com.utec.inventario.dto.response.UsuarioResponse;
import com.utec.inventario.mapper.UsuarioMapper;
import com.utec.inventario.mapper.UsuarioLaboratorioMapper;
import com.utec.inventario.security.JwtService;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.UsuarioService;
import com.utec.inventario.service.AlcanceLaboratorioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final UsuarioMapper mapper;
    private final AlcanceLaboratorioService alcanceLaboratorioService;
    private final UsuarioLaboratorioMapper asignacionMapper;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            UsuarioService usuarioService, UsuarioMapper mapper,
            AlcanceLaboratorioService alcanceLaboratorioService,
            UsuarioLaboratorioMapper asignacionMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.mapper = mapper;
        this.alcanceLaboratorioService = alcanceLaboratorioService;
        this.asignacionMapper = asignacionMapper;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        // BCrypt compara como máximo 72 bytes; evita aceptar sufijos truncados.
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }
        String userName = request.getUserName().trim().toLowerCase(Locale.ROOT);
        Authentication authentication = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userName, request.getPassword()));
        UserInfoDetails principal = (UserInfoDetails) authentication.getPrincipal();
        Usuario usuario = this.usuarioService.obtenerUsuario(principal.getId());
        return AuthResponse.builder()
                .accessToken(this.jwtService.generateToken(principal))
                .tokenType("Bearer")
                .expiresIn(this.jwtService.getExpirationSeconds())
                .usuario(this.mapper.toResponse(usuario))
                .build();
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UsuarioResponse me(@AuthenticationPrincipal UserInfoDetails principal) {
        return this.mapper.toResponse(this.usuarioService.obtenerUsuario(principal.getId()));
    }

    @GetMapping("/me/laboratorios")
    @ResponseStatus(HttpStatus.OK)
    public AlcanceLaboratoriosResponse meLaboratorios(@AuthenticationPrincipal UserInfoDetails principal) {
        return this.asignacionMapper.toResponse(
                this.alcanceLaboratorioService.obtenerAlcanceEfectivo(principal.getId()));
    }
}
