package com.utec.inventario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.UsuarioEntity;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {

    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioEntity> findByUserNameIgnoreCase(String userName);

    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioEntity> findByIdUsuarioAndActivoTrueAndRolActivoTrue(Integer idUsuario);

    boolean existsByUserNameIgnoreCase(String userName);
}
