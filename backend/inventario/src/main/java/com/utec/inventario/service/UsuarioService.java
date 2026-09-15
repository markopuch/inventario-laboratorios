package com.utec.inventario.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.UsuarioMapper;
import com.utec.inventario.repository.UsuarioRepository;
import com.utec.inventario.security.UserInfoDetails;

@Service
@Transactional(readOnly = true)
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    public UserInfoDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        if (userName == null || userName.isBlank()) {
            throw new UsernameNotFoundException("Credenciales inválidas.");
        }

        String nombreNormalizado = userName.trim().toLowerCase(Locale.ROOT);
        UsuarioEntity usuario = this.usuarioRepository.findByUserNameIgnoreCase(nombreNormalizado)
                .filter(entity -> entity.isActivo() && entity.getRol().isActivo())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));

        return new UserInfoDetails(usuario);
    }

    public Usuario obtenerUsuario(Integer idUsuario) {
        UsuarioEntity usuario = this.usuarioRepository
                .findByIdUsuarioAndActivoTrueAndRolActivoTrue(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario activo con ese ID."));

        return this.usuarioMapper.convert(usuario);
    }
}
