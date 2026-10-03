package com.utec.inventario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.RolEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface RolRepository extends JpaRepository<RolEntity, Integer> {

    Optional<RolEntity> findByNombreAndActivoTrue(String nombre);

    // La fila estable ADMIN serializa cambios que podrían eliminar al último administrador.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RolEntity r where r.nombre = :nombre and r.activo = true")
    Optional<RolEntity> findActiveForUpdateByNombre(@Param("nombre") String nombre);
}
