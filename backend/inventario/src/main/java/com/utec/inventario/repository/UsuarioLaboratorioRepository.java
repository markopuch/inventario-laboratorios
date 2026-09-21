package com.utec.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utec.inventario.entity.UsuarioLaboratorioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioId;

@Repository
public interface UsuarioLaboratorioRepository
        extends JpaRepository<UsuarioLaboratorioEntity, UsuarioLaboratorioId> {

    // No cargar usuario: su resumen público se consulta sin password_hash.
    @EntityGraph(attributePaths = {"laboratorio", "laboratorio.area", "laboratorio.area.sede"})
    List<UsuarioLaboratorioEntity> findAllByUsuario_IdUsuarioAndActivoTrueOrderByLaboratorio_IdLaboratorioAsc(
            Integer idUsuario);

    @EntityGraph(attributePaths = {"laboratorio", "laboratorio.area", "laboratorio.area.sede"})
    List<UsuarioLaboratorioEntity> findAllByUsuario_IdUsuarioOrderByLaboratorio_IdLaboratorioAsc(
            Integer idUsuario);

    @EntityGraph(attributePaths = {"laboratorio", "laboratorio.area", "laboratorio.area.sede"})
    List<UsuarioLaboratorioEntity>
            findAllByUsuario_IdUsuarioAndActivoTrueAndLaboratorio_ActivoTrueOrderByLaboratorio_IdLaboratorioAsc(
                    Integer idUsuario);

    @EntityGraph(attributePaths = {"laboratorio", "laboratorio.area", "laboratorio.area.sede"})
    List<UsuarioLaboratorioEntity> findAllByLaboratorio_IdLaboratorioAndActivoTrueOrderByUsuario_IdUsuarioAsc(
            Integer idLaboratorio);

    boolean existsByUsuario_IdUsuarioAndLaboratorio_IdLaboratorioAndActivoTrue(
            Integer idUsuario, Integer idLaboratorio);

    boolean existsByUsuario_IdUsuarioAndLaboratorio_IdLaboratorioAndActivoTrueAndLaboratorio_ActivoTrue(
            Integer idUsuario, Integer idLaboratorio);

    boolean existsByLaboratorio_IdLaboratorioAndActivoTrue(Integer idLaboratorio);
}

