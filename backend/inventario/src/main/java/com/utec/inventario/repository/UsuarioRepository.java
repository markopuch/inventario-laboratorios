package com.utec.inventario.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.UsuarioEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {

    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioEntity> findByUserNameIgnoreCase(String userName);

    @EntityGraph(attributePaths = "rol")
    Optional<UsuarioEntity> findByIdUsuarioAndActivoTrueAndRolActivoTrue(Integer idUsuario);

    boolean existsByUserNameIgnoreCase(String userName);

    // Bloquea solo Usuario, incluso inactivo, sin seleccionar password_hash ni unirse al rol.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u.idUsuario from UsuarioEntity u where u.idUsuario = :idUsuario")
    Optional<Integer> findIdForUpdateByIdUsuario(@Param("idUsuario") Integer idUsuario);

    // Equipo comparte una lectura estable del actor con las FK al responsable.
    // El PUT de asignaciones requiere el bloqueo exclusivo y espera hasta el commit.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u.idUsuario from UsuarioEntity u where u.idUsuario = :idUsuario")
    Optional<Integer> findIdForShareByIdUsuario(@Param("idUsuario") Integer idUsuario);

    // Proyección pública: administrar asignaciones no necesita credenciales ni la Entity usuario.
    @Query("""
            select new com.utec.inventario.domain.Usuario(
                u.idUsuario, u.userName, u.nombre, u.apellido, u.email,
                r.nombre, u.activo, u.fechaCreacion)
            from UsuarioEntity u join u.rol r
            where u.idUsuario = :idUsuario
            """)
    Optional<Usuario> findPublicByIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("""
            select new com.utec.inventario.domain.Usuario(
                u.idUsuario, u.userName, u.nombre, u.apellido, u.email,
                r.nombre, u.activo, u.fechaCreacion)
            from UsuarioEntity u join u.rol r
            where u.idUsuario in :idsUsuario
            """)
    List<Usuario> findPublicByIdUsuarioIn(@Param("idsUsuario") Collection<Integer> idsUsuario);

    @Query("""
            select new com.utec.inventario.domain.Usuario(
                u.idUsuario, u.userName, u.nombre, u.apellido, u.email,
                r.nombre, u.activo, u.fechaCreacion)
            from UsuarioEntity u join u.rol r
            where u.idUsuario = :idUsuario and u.activo = true and r.activo = true
            """)
    Optional<Usuario> findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(
            @Param("idUsuario") Integer idUsuario);
}
