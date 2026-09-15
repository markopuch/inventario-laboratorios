package com.utec.inventario.security;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.utec.inventario.entity.UsuarioEntity;

public class UserInfoDetails implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String username;
    private @Nullable String password;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public UserInfoDetails(UsuarioEntity usuario) {
        this.id = usuario.getIdUsuario();
        this.username = usuario.getUserName();
        this.password = usuario.getPasswordHash();
        this.enabled = usuario.isActivo() && usuario.getRol().isActivo();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()));
    }

    public Integer getId() {
        return this.id;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    @JsonIgnore
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
