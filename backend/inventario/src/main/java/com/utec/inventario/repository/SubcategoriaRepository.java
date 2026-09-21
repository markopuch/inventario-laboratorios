package com.utec.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.SubcategoriaEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface SubcategoriaRepository extends JpaRepository<SubcategoriaEntity, Integer> {

    @EntityGraph(attributePaths = "categoria")
    List<SubcategoriaEntity> findAllByActivoTrueOrderByIdSubcategoriaAsc();

    @EntityGraph(attributePaths = "categoria")
    Optional<SubcategoriaEntity> findByIdSubcategoriaAndActivoTrue(Integer idSubcategoria);

    // Bloquea únicamente el hijo; el Service bloquea el padre destino por separado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SubcategoriaEntity> findForUpdateByIdSubcategoriaAndActivoTrue(Integer idSubcategoria);

    @EntityGraph(attributePaths = "categoria")
    List<SubcategoriaEntity> findAllByCategoria_IdCategoriaAndActivoTrueOrderByIdSubcategoriaAsc(Integer idCategoria);

    boolean existsByCategoria_IdCategoriaAndNombreIgnoreCase(Integer idCategoria, String nombre);

    boolean existsByCategoria_IdCategoriaAndNombreIgnoreCaseAndIdSubcategoriaNot(
            Integer idCategoria, String nombre, Integer idSubcategoria);

    boolean existsByCategoria_IdCategoriaAndActivoTrue(Integer idCategoria);
}
