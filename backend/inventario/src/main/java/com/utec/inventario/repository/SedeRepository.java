package com.utec.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.SedeEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface SedeRepository extends JpaRepository<SedeEntity, Integer> {

    List<SedeEntity> findAllByActivoTrueOrderByIdSedeAsc();

    Optional<SedeEntity> findByIdSedeAndActivoTrue(Integer idSede);

    // Serializa las escrituras de Sede y la validación del padre al crear o mover áreas.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SedeEntity> findForUpdateByIdSedeAndActivoTrue(Integer idSede);
}
