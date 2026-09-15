package com.utec.inventario.config;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.utec.inventario.entity.RolEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.repository.RolRepository;
import com.utec.inventario.repository.UsuarioRepository;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DemoUsuariosConfig {

    @Bean
    public ApplicationRunner crearUsuariosDemo(UsuarioRepository usuarioRepository,
            RolRepository rolRepository, PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager,
            @Value("${app.demo-users.password}") String password) {
        return args -> {
            if (password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Configura una contraseña demo de entre 1 y 72 bytes UTF-8.");
            }

            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.executeWithoutResult(status -> {
                this.crearUsuario(usuarioRepository, rolRepository, passwordEncoder,
                        "marko", "Marko", "ADMIN", password);
                this.crearUsuario(usuarioRepository, rolRepository, passwordEncoder,
                        "aldo", "Aldo", "GESTOR", password);
                this.crearUsuario(usuarioRepository, rolRepository, passwordEncoder,
                        "romel", "Romel", "LECTOR", password);
            });
        };
    }

    private void crearUsuario(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, String userName, String nombre, String nombreRol,
            String password) {
        // Reiniciar el perfil dev no restablece contraseñas ni reactiva cuentas existentes.
        if (usuarioRepository.existsByUserNameIgnoreCase(userName)) {
            return;
        }

        RolEntity rol = rolRepository.findByNombreAndActivoTrue(nombreRol)
                .orElseThrow(() -> new IllegalStateException("No existe el rol activo " + nombreRol + "."));

        UsuarioEntity usuario = UsuarioEntity.builder()
                .userName(userName)
                .nombre(nombre)
                .apellido("Demo")
                .email(userName + "@inventario.test")
                .passwordHash(passwordEncoder.encode(password))
                .activo(true)
                .rol(rol)
                .build();

        usuarioRepository.saveAndFlush(usuario);
    }
}
