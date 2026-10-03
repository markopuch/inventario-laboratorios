package com.utec.inventario.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.AccionAuditoria;
import com.utec.inventario.domain.RolUsuario;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.RolEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.PasswordUsuarioInvalidaException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.UsuarioMapper;
import com.utec.inventario.repository.RolRepository;
import com.utec.inventario.repository.UsuarioRepository;
import com.utec.inventario.security.UserInfoDetails;

@Service
@Transactional(readOnly = true)
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    @Autowired
    public AdminUsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            UsuarioMapper mapper, PasswordEncoder passwordEncoder, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    public List<Usuario> listarUsuarios() {
        return this.mapper.convert(this.usuarioRepository.findAllByOrderByIdUsuarioAsc());
    }

    public Usuario obtenerUsuario(Integer id) {
        return this.usuarioRepository.findPublicByIdUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con ese ID."));
    }

    @Transactional
    public Usuario crearUsuario(Usuario usuario, String password) {
        this.validarPassword(password);
        this.bloquearAdministracion();
        String userName = usuario.getUserName().trim().toLowerCase(Locale.ROOT);
        String email = usuario.getEmail().trim().toLowerCase(Locale.ROOT);
        if (this.usuarioRepository.existsByUserNameIgnoreCase(userName)) {
            throw new ConflictException("Ya existe un usuario con ese nombre de usuario.");
        }
        if (this.usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe un usuario con ese email.");
        }

        RolEntity rol = this.buscarRolActivo(usuario.getRol());
        UsuarioEntity nuevo = UsuarioEntity.builder()
                .userName(userName)
                .nombre(usuario.getNombre().trim())
                .apellido(usuario.getApellido().trim())
                .email(email)
                .cargo(this.normalizarCargo(usuario.getCargo()))
                .passwordHash(this.passwordEncoder.encode(password))
                .rol(rol)
                .activo(usuario.isActivo())
                .build();
        UsuarioEntity guardado = this.usuarioRepository.saveAndFlush(nuevo);
        this.auditoriaService.registrar(AccionAuditoria.CREAR, "usuario", guardado.getIdUsuario(),
                "Usuario creado; rol=" + rol.getNombre() + "; activo=" + guardado.isActivo());
        return this.mapper.convert(guardado);
    }

    @Transactional
    public Usuario actualizarUsuario(Integer id, Usuario cambios) {
        this.bloquearAdministracion();
        UsuarioEntity usuario = this.buscarParaModificar(id);
        String email = cambios.getEmail().trim().toLowerCase(Locale.ROOT);
        if (this.usuarioRepository.existsByEmailIgnoreCaseAndIdUsuarioNot(email, id)) {
            throw new ConflictException("Ya existe un usuario con ese email.");
        }
        usuario.setNombre(cambios.getNombre().trim());
        usuario.setApellido(cambios.getApellido().trim());
        usuario.setEmail(email);
        usuario.setCargo(this.normalizarCargo(cambios.getCargo()));
        UsuarioEntity guardado = this.usuarioRepository.saveAndFlush(usuario);
        this.auditoriaService.registrar(AccionAuditoria.EDITAR, "usuario", id,
                "Campos actualizados: nombre, apellido, email, cargo");
        return this.mapper.convert(guardado);
    }

    @Transactional
    public Usuario cambiarEstado(Integer id, boolean activo) {
        this.bloquearAdministracion();
        UsuarioEntity usuario = this.buscarParaModificar(id);
        if (activo && !usuario.getRol().isActivo()) {
            throw new ConflictException("No se puede activar un usuario cuyo rol está inactivo.");
        }
        if (!activo) {
            this.protegerUltimoAdministrador(usuario);
        }
        boolean cambia = usuario.isActivo() != activo;
        usuario.setActivo(activo);
        UsuarioEntity guardado = this.usuarioRepository.saveAndFlush(usuario);
        if (cambia) {
            this.auditoriaService.registrar(activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR,
                    "usuario", id, "activo=" + activo);
        }
        // Las asignaciones se conservan. La autenticación y el alcance rechazan usuarios inactivos.
        return this.mapper.convert(guardado);
    }

    @Transactional
    public Usuario cambiarRol(Integer id, RolUsuario nuevoRol) {
        this.bloquearAdministracion();
        UsuarioEntity usuario = this.buscarParaModificar(id);
        RolEntity rol = this.buscarRolActivo(nuevoRol.name());
        if (nuevoRol != RolUsuario.ADMIN) {
            this.protegerUltimoAdministrador(usuario);
        }
        String rolAnterior = usuario.getRol().getNombre();
        usuario.setRol(rol);
        UsuarioEntity guardado = this.usuarioRepository.saveAndFlush(usuario);
        if (!rolAnterior.equals(rol.getNombre())) {
            this.auditoriaService.registrar(AccionAuditoria.CAMBIAR_ROL, "usuario", id,
                    "rol=" + rolAnterior + " -> " + rol.getNombre());
        }
        return this.mapper.convert(guardado);
    }

    @Transactional
    public void cambiarPassword(Integer id, String password) {
        this.validarPassword(password);
        this.bloquearAdministracion();
        UsuarioEntity usuario = this.buscarParaModificar(id);
        usuario.setPasswordHash(this.passwordEncoder.encode(password));
        this.usuarioRepository.saveAndFlush(usuario);
        this.auditoriaService.registrar(AccionAuditoria.RESTABLECER_CONTRASENA, "usuario", id,
                "Contraseña restablecida");
    }

    private UsuarioEntity buscarParaModificar(Integer id) {
        this.usuarioRepository.findIdForUpdateByIdUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con ese ID."));
        return this.usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con ese ID."));
    }

    private RolEntity buscarRolActivo(String nombre) {
        return this.rolRepository.findByNombreAndActivoTrue(nombre)
                .orElseThrow(() -> new ConflictException("El rol seleccionado no existe o está inactivo."));
    }

    private void bloquearAdministracion() {
        this.rolRepository.findActiveForUpdateByNombre(RolUsuario.ADMIN.name())
                .orElseThrow(() -> new ConflictException("El rol ADMIN no está disponible."));
        // El filtro JWT autentica antes de que una petición pueda esperar este bloqueo.
        // Reconsulta al actor bajo el mismo lock que serializa los cambios de rol/estado.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserInfoDetails actor) {
            Usuario vigente = this.usuarioRepository.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(actor.getId())
                    .orElseThrow(() -> new BadCredentialsException("El usuario no tiene acceso vigente."));
            if (!RolUsuario.ADMIN.name().equals(vigente.getRol())) {
                throw new AccessDeniedException("La administración de usuarios requiere el rol ADMIN.");
            }
        }
    }

    private void protegerUltimoAdministrador(UsuarioEntity usuario) {
        if (usuario.isActivo() && RolUsuario.ADMIN.name().equals(usuario.getRol().getNombre())
                && this.usuarioRepository.countByActivoTrueAndRol_NombreAndRol_ActivoTrue(
                        RolUsuario.ADMIN.name()) <= 1) {
            throw new ConflictException("No se puede desactivar ni degradar al último ADMIN activo.");
        }
    }

    private String normalizarCargo(String cargo) {
        return cargo == null ? null : cargo.trim();
    }

    private void validarPassword(String password) {
        if (password == null || password.isBlank() || password.length() < 4 || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordUsuarioInvalidaException();
        }
    }
}
