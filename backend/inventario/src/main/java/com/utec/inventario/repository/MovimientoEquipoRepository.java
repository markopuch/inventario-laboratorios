package com.utec.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.MovimientoEquipoEntity;

@Repository
public interface MovimientoEquipoRepository extends JpaRepository<MovimientoEquipoEntity, Integer> {

    @EntityGraph(attributePaths = {"equipo", "laboratorioOrigen", "laboratorioDestino"})
    List<MovimientoEquipoEntity> findAllByEquipo_IdEquipoOrderByFechaMovimientoDescIdMovimientoDesc(Integer idEquipo);

    // LEFT JOIN conserva los registros legacy cuyo origen es NULL.
    @EntityGraph(attributePaths = {"equipo", "laboratorioOrigen", "laboratorioDestino"})
    @Query("""
            select m from MovimientoEquipoEntity m
            left join m.laboratorioOrigen origen
            join m.laboratorioDestino destino
            where (:idLaboratorio is null or origen.idLaboratorio = :idLaboratorio
                   or destino.idLaboratorio = :idLaboratorio)
            order by m.fechaMovimiento desc, m.idMovimiento desc
            """)
    List<MovimientoEquipoEntity> findAllFiltrados(@Param("idLaboratorio") Integer idLaboratorio);

    @EntityGraph(attributePaths = {"equipo", "laboratorioOrigen", "laboratorioDestino"})
    @Query("""
            select m from MovimientoEquipoEntity m
            left join m.laboratorioOrigen origen
            join m.laboratorioDestino destino
            where (origen.idLaboratorio in :idsLaboratorio or destino.idLaboratorio in :idsLaboratorio)
              and (:idEquipo is null or m.equipo.idEquipo = :idEquipo)
              and (:idLaboratorio is null or origen.idLaboratorio = :idLaboratorio
                   or destino.idLaboratorio = :idLaboratorio)
            order by m.fechaMovimiento desc, m.idMovimiento desc
            """)
    List<MovimientoEquipoEntity> findAllVisibles(@Param("idEquipo") Integer idEquipo,
            @Param("idLaboratorio") Integer idLaboratorio,
            @Param("idsLaboratorio") List<Integer> idsLaboratorio);
}

