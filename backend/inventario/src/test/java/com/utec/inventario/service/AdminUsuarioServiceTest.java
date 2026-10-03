package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.utec.inventario.domain.AccionAuditoria;
import com.utec.inventario.domain.RolUsuario;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.RolEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.PasswordUsuarioInvalidaException;
import com.utec.inventario.mapper.UsuarioMapper;
import com.utec.inventario.repository.RolRepository;
import com.utec.inventario.repository.UsuarioRepository;
import com.utec.inventario.security.UserInfoDetails;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private RolRepository roles;
    @Mock private PasswordEncoder encoder;
    @Mock private AuditoriaService auditoria;
    private AdminUsuarioService service;

    @BeforeEach
    void preparar() {
        service = new AdminUsuarioService(usuarios, roles, Mappers.getMapper(UsuarioMapper.class), encoder, auditoria);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaIdentidadNormalizadaCifraPasswordSinAlterarlaYAuditaSinSecretos() {
        RolEntity lector = rol("LECTOR");
        when(roles.findActiveForUpdateByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN")));
        when(roles.findByNombreAndActivoTrue("LECTOR")).thenReturn(Optional.of(lector));
        String password = "  clave con espacios  ";
        when(encoder.encode(password)).thenReturn("hash-seguro");
        when(usuarios.saveAndFlush(any())).thenAnswer(call -> {
            UsuarioEntity entity = call.getArgument(0);
            entity.setIdUsuario(8);
            return entity;
        });
        Usuario input = Usuario.builder().userName("  Nuevo  ").nombre("  Ana ").apellido(" Pérez  ")
                .email(" ANA@INVENTARIO.TEST ").cargo(" Docente ").rol("LECTOR").activo(true).build();

        Usuario result = service.crearUsuario(input, password);

        assertEquals("nuevo", result.getUserName());
        assertEquals("ana@inventario.test", result.getEmail());
        assertEquals("Docente", result.getCargo());
        ArgumentCaptor<UsuarioEntity> captured = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarios).saveAndFlush(captured.capture());
        assertEquals("hash-seguro", captured.getValue().getPasswordHash());
        verify(encoder).encode(password);
        verify(auditoria).registrar(AccionAuditoria.CREAR, "usuario", 8,
                "Usuario creado; rol=LECTOR; activo=true");
    }

    @Test
    void rechazaPasswordsQueBcryptTruncariaAntesDeTocarPersistencia() {
        Usuario input = Usuario.builder().build();
        for (String password : new String[] {"", "abc", "a".repeat(73), "ñ".repeat(37)}) {
            assertThrows(PasswordUsuarioInvalidaException.class, () -> service.crearUsuario(input, password));
        }
        assertThrows(PasswordUsuarioInvalidaException.class, () -> service.cambiarPassword(2, null));
        verifyNoInteractions(usuarios, roles, encoder, auditoria);
    }

    @Test
    void duplicadosIncluyenUsuariosInactivosYSonConflictos() {
        Usuario input = Usuario.builder().userName("Existente").email("otro@inventario.test").build();
        when(roles.findActiveForUpdateByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN")));
        when(usuarios.existsByUserNameIgnoreCase("existente")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.crearUsuario(input, "1234"));
        verify(usuarios, never()).saveAndFlush(any());
        verifyNoInteractions(encoder, auditoria);
    }

    @Test
    void bloqueaFilaAdminAntesDelUsuarioYProtegeUltimoAdministrador() {
        UsuarioEntity admin = usuario(2, "ADMIN", true);
        when(roles.findActiveForUpdateByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN")));
        configurarUsuario(admin);
        when(usuarios.countByActivoTrueAndRol_NombreAndRol_ActivoTrue("ADMIN")).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.cambiarEstado(2, false));

        var orden = inOrder(roles, usuarios);
        orden.verify(roles).findActiveForUpdateByNombre("ADMIN");
        orden.verify(usuarios).findIdForUpdateByIdUsuario(2);
        assertTrue(admin.isActivo());
        verify(usuarios, never()).saveAndFlush(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void noPermiteDegradarUltimoAdminPeroPermiteSiQuedaOtro() {
        UsuarioEntity admin = usuario(2, "ADMIN", true);
        when(roles.findActiveForUpdateByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN")));
        when(roles.findByNombreAndActivoTrue("LECTOR")).thenReturn(Optional.of(rol("LECTOR")));
        configurarUsuario(admin);
        when(usuarios.countByActivoTrueAndRol_NombreAndRol_ActivoTrue("ADMIN")).thenReturn(1L, 2L);
        assertThrows(ConflictException.class, () -> service.cambiarRol(2, RolUsuario.LECTOR));
        when(usuarios.saveAndFlush(admin)).thenReturn(admin);
        assertEquals("LECTOR", service.cambiarRol(2, RolUsuario.LECTOR).getRol());
        verify(auditoria).registrar(AccionAuditoria.CAMBIAR_ROL, "usuario", 2, "rol=ADMIN -> LECTOR");
    }

    @Test
    void desactivaSinBorrarAsignacionesYNoGeneraEventoDuplicado() {
        UsuarioEntity lector = usuario(2, "LECTOR", true);
        when(roles.findActiveForUpdateByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN")));
        configurarUsuario(lector);
        when(usuarios.saveAndFlush(lector)).thenReturn(lector);
        assertFalse(service.cambiarEstado(2, false).isActivo());
        assertFalse(service.cambiarEstado(2, false).isActivo());
        verify(auditoria, times(1)).registrar(AccionAuditoria.DESACTIVAR, "usuario", 2, "activo=false");
        verify(usuarios, never()).delete(any());
        verify(usuarios, never()).deleteById(any());
    }

    @Test
    void restablecePasswordYAuditaSoloLaAccion() {
        UsuarioEntity lector = usuario(2, "LECTOR", false);
        when(roles.findActiveForUpdateByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN")));
        configurarUsuario(lector);
        when(encoder.encode("nueva clave")).thenReturn("nuevo-hash");
        service.cambiarPassword(2, "nueva clave");
        assertEquals("nuevo-hash", lector.getPasswordHash());
        verify(auditoria).registrar(AccionAuditoria.RESTABLECER_CONTRASENA, "usuario", 2,
                "Contraseña restablecida");
        verify(roles).findActiveForUpdateByNombre("ADMIN");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void revalidaActorTrasBloqueoYRechazaAdminInactivoODegradado(boolean degradado) {
        UserInfoDetails actorAntesDeEsperar = new UserInfoDetails(usuario(70, "ADMIN", true));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actorAntesDeEsperar, null, actorAntesDeEsperar.getAuthorities()));
        when(roles.findActiveForUpdateByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN")));
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(70))
                .thenReturn(degradado ? Optional.of(Usuario.builder().id(70).rol("LECTOR").activo(true).build())
                        : Optional.empty());

        if (degradado) {
            assertThrows(AccessDeniedException.class, () -> service.cambiarPassword(2, "1234"));
        } else {
            assertThrows(BadCredentialsException.class, () -> service.cambiarPassword(2, "1234"));
        }
        var orden = inOrder(roles, usuarios);
        orden.verify(roles).findActiveForUpdateByNombre("ADMIN");
        orden.verify(usuarios).findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(70);
        verify(usuarios, never()).findIdForUpdateByIdUsuario(anyInt());
        verify(usuarios, never()).saveAndFlush(any());
        verifyNoInteractions(encoder, auditoria);
    }

    private void configurarUsuario(UsuarioEntity usuario) {
        when(usuarios.findIdForUpdateByIdUsuario(usuario.getIdUsuario()))
                .thenReturn(Optional.of(usuario.getIdUsuario()));
        when(usuarios.findById(usuario.getIdUsuario())).thenReturn(Optional.of(usuario));
    }

    private RolEntity rol(String nombre) {
        return RolEntity.builder().idRol(1).nombre(nombre).activo(true).build();
    }

    private UsuarioEntity usuario(Integer id, String nombreRol, boolean activo) {
        return UsuarioEntity.builder().idUsuario(id).userName("usuario").nombre("Nombre").apellido("Apellido")
                .email("usuario@inventario.test").rol(rol(nombreRol)).activo(activo).build();
    }
}
