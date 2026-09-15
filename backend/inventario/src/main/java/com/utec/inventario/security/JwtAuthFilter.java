package com.utec.inventario.security;

import java.io.IOException;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.utec.inventario.service.UsuarioService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final SecurityErrorHandler securityErrorHandler;

    @Autowired
    public JwtAuthFilter(JwtService jwtService, UsuarioService usuarioService,
            SecurityErrorHandler securityErrorHandler) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null) {
            try {
                Enumeration<String> headers = request.getHeaders("Authorization");
                headers.nextElement();
                if (headers.hasMoreElements() || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    throw new BadCredentialsException("El encabezado de autenticación es inválido.");
                }

                String token = authorizationHeader.substring(7);
                if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
                    throw new BadCredentialsException("El token es inválido.");
                }

                String username = this.jwtService.extractUsername(token);
                if (username == null || username.isBlank()) {
                    throw new BadCredentialsException("El token es inválido.");
                }

                // El rol y el estado se obtienen de PostgreSQL en cada solicitud.
                UserInfoDetails user = this.usuarioService.loadUserByUsername(username);
                if (!this.jwtService.validateToken(token, user)) {
                    throw new BadCredentialsException("El token es inválido.");
                }

                user.eraseCredentials();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                this.securityErrorHandler.commence(request, response,
                        new BadCredentialsException("La autenticación es inválida."));
                return;
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
                log.error("No se pudo verificar el usuario durante la autenticación JWT.", exception);
                this.securityErrorHandler.internalServerError(request, response);
                return;
            }
        }

        // Los errores del controlador y del servicio mantienen su manejo habitual.
        chain.doFilter(request, response);
    }
}
