package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Autenticación", description = "Acceso JWT, perfil propio y alcance efectivo de laboratorios.")
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

    @Operation(summary = "Iniciar sesión y obtener JWT",
            description = "Acceso público con userName y password. Normaliza el usuario; no recorta la contraseña. Devuelve Bearer, vigencia y perfil. No renueva ni revoca tokens previos.")
    @SecurityRequirements
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

    @Operation(summary = "Consultar el perfil autenticado",
            description = "ADMIN, GESTOR y LECTOR. Devuelve el usuario vigente identificado por el JWT; no permite seleccionar otro usuario.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UsuarioResponse me(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal) {
        return this.mapper.toResponse(this.usuarioService.obtenerUsuario(principal.getId()));
    }

    @Operation(summary = "Consultar el alcance propio de laboratorios",
            description = "ADMIN obtiene alcance global y laboratorios activos. GESTOR y LECTOR obtienen solo asignaciones activas a laboratorios activos. Usa el estado vigente sin exigir un JWT nuevo.")
    @GetMapping("/me/laboratorios")
    @ResponseStatus(HttpStatus.OK)
    public AlcanceLaboratoriosResponse meLaboratorios(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal) {
        return this.asignacionMapper.toResponse(
                this.alcanceLaboratorioService.obtenerAlcanceEfectivo(principal.getId()));
    }
}
