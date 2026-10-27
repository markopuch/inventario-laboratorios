package com.utec.inventario.config;

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
import com.utec.inventario.security.PasswordPolicy;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DemoUsuariosConfig {

    @Bean
    public ApplicationRunner crearUsuariosDemo(UsuarioRepository usuarioRepository,
            RolRepository rolRepository, PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager,
            @Value("${app.demo-users.password}") String password) {
        return args -> {
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
        if (!PasswordPolicy.isValid(password)) {
            throw new IllegalStateException("Configura DEMO_USER_PASSWORD. " + PasswordPolicy.MESSAGE);
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
