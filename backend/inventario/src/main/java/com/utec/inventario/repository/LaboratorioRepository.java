package com.utec.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.LaboratorioEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface LaboratorioRepository extends JpaRepository<LaboratorioEntity, Integer> {

    @EntityGraph(attributePaths = {"area", "area.sede"})
    List<LaboratorioEntity> findAllByActivoTrueOrderByIdLaboratorioAsc();

    @EntityGraph(attributePaths = {"area", "area.sede"})
    Optional<LaboratorioEntity> findByIdLaboratorioAndActivoTrue(Integer idLaboratorio);

    // El bloqueo no incluye joins; el Service bloquea el padre destino por separado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LaboratorioEntity> findForUpdateByIdLaboratorioAndActivoTrue(Integer idLaboratorio);

    @EntityGraph(attributePaths = {"area", "area.sede"})
    List<LaboratorioEntity> findAllByArea_IdAreaAndActivoTrueOrderByIdLaboratorioAsc(Integer idArea);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdLaboratorioNot(String codigo, Integer idLaboratorio);

    boolean existsByArea_IdAreaAndActivoTrue(Integer idArea);
}
