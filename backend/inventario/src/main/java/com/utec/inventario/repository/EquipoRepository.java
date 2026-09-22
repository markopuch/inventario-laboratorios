package com.utec.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.entity.EquipoEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface EquipoRepository extends JpaRepository<EquipoEntity, Integer> {

    // Responsable se resuelve por proyección pública; no se carga su Entity ni su hash.
    @EntityGraph(attributePaths = {"subcategoria", "laboratorio"})
    Optional<EquipoEntity> findByIdEquipo(Integer idEquipo);

    // Sin joins: bloquea exclusivamente Equipo, incluido estado BAJA.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquipoEntity> findForUpdateByIdEquipo(Integer idEquipo);

    @EntityGraph(attributePaths = {"subcategoria", "laboratorio"})
    @Query("""
            select e from EquipoEntity e
            where (:estado is null or e.estado = :estado)
              and (:idLaboratorio is null or e.laboratorio.idLaboratorio = :idLaboratorio)
              and (:idSubcategoria is null or e.subcategoria.idSubcategoria = :idSubcategoria)
              and (:requiereMantenimiento is null or e.requiereMantenimiento = :requiereMantenimiento)
            order by e.idEquipo
            """)
    List<EquipoEntity> findAllFiltrados(@Param("estado") EstadoEquipo estado,
            @Param("idLaboratorio") Integer idLaboratorio,
            @Param("idSubcategoria") Integer idSubcategoria,
            @Param("requiereMantenimiento") Boolean requiereMantenimiento);

    @EntityGraph(attributePaths = {"subcategoria", "laboratorio"})
    @Query("""
            select e from EquipoEntity e
            where e.laboratorio.idLaboratorio in :idsLaboratorio
              and e.laboratorio.activo = true
              and (:estado is null or e.estado = :estado)
              and (:idLaboratorio is null or e.laboratorio.idLaboratorio = :idLaboratorio)
              and (:idSubcategoria is null or e.subcategoria.idSubcategoria = :idSubcategoria)
              and (:requiereMantenimiento is null or e.requiereMantenimiento = :requiereMantenimiento)
            order by e.idEquipo
            """)
    List<EquipoEntity> findAllFiltradosEnLaboratorios(@Param("estado") EstadoEquipo estado,
            @Param("idLaboratorio") Integer idLaboratorio,
            @Param("idSubcategoria") Integer idSubcategoria,
            @Param("requiereMantenimiento") Boolean requiereMantenimiento,
            @Param("idsLaboratorio") List<Integer> idsLaboratorio);

    boolean existsByCodigoInterno(String codigoInterno);

    boolean existsBySerieUtec(String serieUtec);

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsBySerieUtecAndIdEquipoNot(String serieUtec, Integer idEquipo);

    boolean existsByNumeroSerieAndIdEquipoNot(String numeroSerie, Integer idEquipo);

    boolean existsBySubcategoria_IdSubcategoriaAndEstadoNot(Integer idSubcategoria, EstadoEquipo estado);

    boolean existsByLaboratorio_IdLaboratorioAndEstadoNot(Integer idLaboratorio, EstadoEquipo estado);
}

