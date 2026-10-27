package com.utec.inventario.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.utec.inventario.exception.LoginRateLimitException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final LoginRateLimiter limiter;
    private final SecurityErrorHandler errors;

    public LoginRateLimitFilter(LoginRateLimiter limiter, SecurityErrorHandler errors) {
        this.limiter = limiter;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !"/api/auth/login".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            // Usa el peer informado por el servidor. No confía en Forwarded/X-Forwarded-For del cliente.
            limiter.checkSource(request.getRemoteAddr());
        } catch (LoginRateLimitException exception) {
            errors.tooManyRequests(request, response, exception.getRetryAfterSeconds());
            return;
        }
        chain.doFilter(request, response);
    }
}
