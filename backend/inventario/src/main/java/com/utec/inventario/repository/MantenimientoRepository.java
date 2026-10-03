package com.utec.inventario.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utec.inventario.domain.EstadoMantenimiento;
import com.utec.inventario.domain.TipoMantenimiento;
import com.utec.inventario.entity.MantenimientoEntity;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;

@Repository
public interface MantenimientoRepository extends JpaRepository<MantenimientoEntity, Integer>,
        JpaSpecificationExecutor<MantenimientoEntity> {
    @EntityGraph(attributePaths = {"equipo", "equipo.laboratorio"})
    Optional<MantenimientoEntity> findByIdMantenimiento(Integer idMantenimiento);

    @Query("select m.equipo.idEquipo from MantenimientoEntity m where m.idMantenimiento = :id")
    Optional<Integer> findIdEquipoByIdMantenimiento(@Param("id") Integer idMantenimiento);

    // Orden de escritura compartido con Equipo/Traslado: actor -> equipo -> mantenimiento.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MantenimientoEntity> findForUpdateByIdMantenimiento(Integer idMantenimiento);

    boolean existsByEquipo_IdEquipoAndEstado(Integer idEquipo, EstadoMantenimiento estado);

    @Override
    @EntityGraph(attributePaths = {"equipo", "equipo.laboratorio"})
    List<MantenimientoEntity> findAll(Specification<MantenimientoEntity> specification);

    default List<MantenimientoEntity> findAllFiltrados(Integer idEquipo, Integer idLaboratorio,
            EstadoMantenimiento estado, TipoMantenimiento tipo, LocalDate fechaDesde, LocalDate fechaHasta,
            List<Integer> idsLaboratorio) {
        return this.findAll((root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            var equipo = root.get("equipo");
            var laboratorio = equipo.get("laboratorio");
            if (idEquipo != null) condiciones.add(builder.equal(equipo.get("idEquipo"), idEquipo));
            if (idLaboratorio != null) condiciones.add(builder.equal(laboratorio.get("idLaboratorio"), idLaboratorio));
            if (estado != null) condiciones.add(builder.equal(root.get("estado"), estado));
            if (tipo != null) condiciones.add(builder.equal(root.get("tipo"), tipo));
            if (fechaDesde != null) condiciones.add(builder.greaterThanOrEqualTo(root.get("fechaProgramada"), fechaDesde));
            if (fechaHasta != null) condiciones.add(builder.lessThanOrEqualTo(root.get("fechaProgramada"), fechaHasta));
            if (idsLaboratorio != null) {
                condiciones.add(laboratorio.get("idLaboratorio").in(idsLaboratorio));
                condiciones.add(builder.isTrue(laboratorio.get("activo")));
            }
            query.orderBy(builder.asc(root.get("fechaProgramada")), builder.asc(root.get("idMantenimiento")));
            return builder.and(condiciones.toArray(Predicate[]::new));
        });
    }
}
