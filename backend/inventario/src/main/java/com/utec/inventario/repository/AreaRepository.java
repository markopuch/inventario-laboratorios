package com.utec.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.AreaEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface AreaRepository extends JpaRepository<AreaEntity, Integer> {

    @EntityGraph(attributePaths = "sede")
    List<AreaEntity> findAllByActivoTrueOrderByIdAreaAsc();

    @EntityGraph(attributePaths = "sede")
    Optional<AreaEntity> findByIdAreaAndActivoTrue(Integer idArea);

    // El bloqueo no incluye joins; el Service bloquea el padre destino por separado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AreaEntity> findForUpdateByIdAreaAndActivoTrue(Integer idArea);

    @EntityGraph(attributePaths = "sede")
    List<AreaEntity> findAllBySede_IdSedeAndActivoTrueOrderByIdAreaAsc(Integer idSede);

    boolean existsBySede_IdSedeAndNombreIgnoreCase(Integer idSede, String nombre);

    boolean existsBySede_IdSedeAndNombreIgnoreCaseAndIdAreaNot(
            Integer idSede, String nombre, Integer idArea);

    boolean existsBySede_IdSedeAndActivoTrue(Integer idSede);
}
