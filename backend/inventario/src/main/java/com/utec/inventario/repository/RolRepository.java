package com.utec.inventario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.RolEntity;

@Repository
public interface RolRepository extends JpaRepository<RolEntity, Integer> {

    Optional<RolEntity> findByNombreAndActivoTrue(String nombre);
}
