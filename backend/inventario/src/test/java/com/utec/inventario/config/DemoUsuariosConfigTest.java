package com.utec.inventario.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.utec.inventario.entity.RolEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.repository.RolRepository;
import com.utec.inventario.repository.UsuarioRepository;
import com.utec.inventario.security.PasswordPolicy;

@ExtendWith(MockitoExtension.class)
class DemoUsuariosConfigTest {

    @Mock private UsuarioRepository users;
    @Mock private RolRepository roles;
    @Mock private PasswordEncoder encoder;
    @Mock private PlatformTransactionManager transactions;
    private final DemoUsuariosConfig config = new DemoUsuariosConfig();

    @BeforeEach
    void createTransaction() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void reiniciarNoValidaNiReescribeClavesCuandoLasCuentasYaExisten() throws Exception {
        when(users.existsByUserNameIgnoreCase(anyString())).thenReturn(true);
        run("corta");
        verify(users, times(3)).existsByUserNameIgnoreCase(anyString());
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(roles, encoder);
        verify(transactions).commit(any());
    }

    @Test
    void cuentaAusenteRechazaClaveDebilAntesDeCifrarOInsertarYRevierte() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> run("corta"));
        assertTrue(error.getMessage().contains(PasswordPolicy.MESSAGE));
        assertFalse(error.getMessage().contains("corta"));
        verifyNoInteractions(roles, encoder);
        verify(users, never()).saveAndFlush(any());
        verify(transactions).rollback(any());
    }

    @Test
    void creaSoloCuentaAusenteConClaveExactaYPoliticaNueva() throws Exception {
        when(users.existsByUserNameIgnoreCase(anyString()))
                .thenAnswer(call -> !"aldo".equals(call.getArgument(0)));
        RolEntity role = RolEntity.builder().idRol(2).nombre("GESTOR").activo(true).build();
        when(roles.findByNombreAndActivoTrue("GESTOR")).thenReturn(Optional.of(role));
        String password = "  una frase de demo  ";
        when(encoder.encode(password)).thenReturn("hash-de-prueba");
        run(password);
        ArgumentCaptor<UsuarioEntity> captured = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(users).saveAndFlush(captured.capture());
        assertEquals("aldo", captured.getValue().getUserName());
        assertEquals("hash-de-prueba", captured.getValue().getPasswordHash());
        assertSame(role, captured.getValue().getRol());
        verify(encoder).encode(password);
        verify(transactions).commit(any());
    }

    private void run(String password) throws Exception {
        config.crearUsuariosDemo(users, roles, encoder, transactions, password)
                .run(new DefaultApplicationArguments(new String[0]));
    }
}
