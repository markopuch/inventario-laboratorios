package com.utec.inventario.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.MovimientoEquipo;
import com.utec.inventario.domain.TipoMovimientoEquipo;
import com.utec.inventario.domain.TrasladoEquipo;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.MovimientoEquipoEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.mapper.MovimientoEquipoMapper;
import com.utec.inventario.repository.EquipoRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.MovimientoEquipoRepository;
import com.utec.inventario.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class MovimientoEquipoService {

    private final MovimientoEquipoRepository movimientoRepository;
    private final EquipoRepository equipoRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlcanceLaboratorioService alcanceService;
    private final MovimientoEquipoMapper mapper;
    private final EquipoMapper equipoMapper;

    @Autowired
    public MovimientoEquipoService(MovimientoEquipoRepository movimientoRepository,
            EquipoRepository equipoRepository, LaboratorioRepository laboratorioRepository,
            UsuarioRepository usuarioRepository, AlcanceLaboratorioService alcanceService,
            MovimientoEquipoMapper mapper, EquipoMapper equipoMapper) {
        this.movimientoRepository = movimientoRepository;
        this.equipoRepository = equipoRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.usuarioRepository = usuarioRepository;
        this.alcanceService = alcanceService;
        this.mapper = mapper;
        this.equipoMapper = equipoMapper;
    }

    @Transactional
    public TrasladoEquipo trasladarEquipo(Integer idUsuario, Integer idEquipo, Integer idLaboratorioDestino,
            String motivo, String ubicacionInternaDestino) {
        // Mismo protocolo de Sprint 5: la revocación de asignaciones espera este actor.
        this.usuarioRepository.findIdForShareByIdUsuario(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene acceso vigente."));
        Usuario actor = this.buscarUsuarioVigente(idUsuario);
        if (!this.esAdmin(actor) && !"GESTOR".equals(actor.getRol())) {
            throw new AccessDeniedException("No tienes permiso para trasladar equipos.");
        }

        EquipoEntity equipo = this.equipoRepository.findForUpdateByIdEquipo(idEquipo)
                .orElseThrow(() -> this.equipoInexistente(idEquipo));
        if (equipo.getEstado() == EstadoEquipo.BAJA) {
            throw new ConflictException("No se puede trasladar un equipo dado de baja.");
        }
        Integer idOrigen = equipo.getLaboratorio().getIdLaboratorio();
        if (idOrigen.equals(idLaboratorioDestino)) {
            throw new ConflictException("El laboratorio destino debe ser diferente al laboratorio actual.");
        }

        // Actor → Equipo → laboratorios por ID; el origen real se obtuvo DESPUÉS de bloquear Equipo.
        Map<Integer, LaboratorioEntity> laboratorios = new LinkedHashMap<>();
        for (Integer idLaboratorio : List.of(idOrigen, idLaboratorioDestino).stream().sorted().toList()) {
            LaboratorioEntity laboratorio = this.laboratorioRepository.findForUpdateByIdLaboratorio(idLaboratorio)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No existe un laboratorio con el ID " + idLaboratorio + "."));
            laboratorios.put(idLaboratorio, laboratorio);
        }
        LaboratorioEntity origen = laboratorios.get(idOrigen);
        LaboratorioEntity destino = laboratorios.get(idLaboratorioDestino);
        if (!destino.isActivo()) {
            throw new ConflictException("El laboratorio destino está inactivo.");
        }
        if (!this.esAdmin(actor)) {
            this.alcanceService.validarAccesoLaboratorio(idUsuario, idOrigen);
            this.alcanceService.validarAccesoLaboratorio(idUsuario, idLaboratorioDestino);
        }

        MovimientoEquipoEntity nuevoMovimiento = MovimientoEquipoEntity.builder()
                .equipo(equipo)
                .laboratorioOrigen(origen)
                .laboratorioDestino(destino)
                .usuarioActor(this.usuarioRepository.getReferenceById(idUsuario))
                .tipoMovimiento(TipoMovimientoEquipo.TRASLADO.name())
                .motivo(motivo.trim())
                .build();

        equipo.setLaboratorio(destino);
        equipo.setUbicacionInterna(this.normalizarUbicacion(ubicacionInternaDestino));
        equipo.setFechaActualizacion(OffsetDateTime.now(ZoneOffset.UTC));
        EquipoEntity equipoGuardado = this.equipoRepository.saveAndFlush(equipo);
        // Si este INSERT falla, la misma transacción revierte también el UPDATE ya enviado.
        MovimientoEquipoEntity movimientoGuardado = this.movimientoRepository.saveAndFlush(nuevoMovimiento);

        Usuario responsable = equipoGuardado.getResponsable() == null ? null
                : this.usuarioRepository.findPublicByIdUsuario(equipoGuardado.getResponsable().getIdUsuario())
                        .orElseThrow(() -> new ResourceNotFoundException("No existe el responsable del equipo."));
        Equipo equipoActualizado = this.equipoMapper.convert(equipoGuardado, responsable);
        return TrasladoEquipo.builder()
                .equipo(equipoActualizado)
                .movimiento(this.mapper.convert(movimientoGuardado, actor))
                .build();
    }

    public List<MovimientoEquipo> listarPorEquipo(Integer idUsuario, Integer idEquipo) {
        Usuario usuario = this.buscarUsuarioVigente(idUsuario);
        // No se usa EquipoService.obtenerEquipo: la historia puede ser visible aunque el equipo ya esté fuera.
        EquipoEntity equipo = this.equipoRepository.findByIdEquipo(idEquipo)
                .orElseThrow(() -> this.equipoInexistente(idEquipo));
        List<MovimientoEquipoEntity> movimientos;
        if (this.esAdmin(usuario)) {
            movimientos = this.movimientoRepository
                    .findAllByEquipo_IdEquipoOrderByFechaMovimientoDescIdMovimientoDesc(idEquipo);
        } else {
            List<Integer> permitidos = this.idsPermitidos(idUsuario);
            movimientos = permitidos.isEmpty() ? List.of()
                    : this.movimientoRepository.findAllVisibles(idEquipo, null, permitidos);
            if (movimientos.isEmpty() && !permitidos.contains(equipo.getLaboratorio().getIdLaboratorio())) {
                throw new AccessDeniedException("No tienes acceso al equipo ni a su historial.");
            }
        }
        return this.convertirMovimientos(movimientos);
    }

    public List<MovimientoEquipo> listarMovimientos(Integer idUsuario, Integer idLaboratorio) {
        Usuario usuario = this.buscarUsuarioVigente(idUsuario);
        List<MovimientoEquipoEntity> movimientos;
        if (this.esAdmin(usuario)) {
            if (idLaboratorio != null && !this.laboratorioRepository.existsById(idLaboratorio)) {
                throw new ResourceNotFoundException("No existe un laboratorio con el ID " + idLaboratorio + ".");
            }
            movimientos = this.movimientoRepository.findAllFiltrados(idLaboratorio);
        } else {
            if (idLaboratorio != null) {
                this.alcanceService.validarAccesoLaboratorio(idUsuario, idLaboratorio);
            }
            List<Integer> permitidos = this.idsPermitidos(idUsuario);
            movimientos = permitidos.isEmpty() ? List.of()
                    : this.movimientoRepository.findAllVisibles(null, idLaboratorio, permitidos);
        }
        return this.convertirMovimientos(movimientos);
    }

    private List<Integer> idsPermitidos(Integer idUsuario) {
        return this.alcanceService.obtenerAlcanceEfectivo(idUsuario).getLaboratorios()
                .stream().map(Laboratorio::getId).toList();
    }

    private List<MovimientoEquipo> convertirMovimientos(List<MovimientoEquipoEntity> movimientos) {
        if (movimientos.isEmpty()) {
            return List.of();
        }
        List<Integer> idsActores = movimientos.stream()
                .map(movimiento -> movimiento.getUsuarioActor().getIdUsuario()).distinct().toList();
        Map<Integer, Usuario> actores = this.usuarioRepository.findPublicByIdUsuarioIn(idsActores).stream()
                .collect(Collectors.toMap(Usuario::getId, usuario -> usuario));
        return movimientos.stream().map(movimiento -> this.mapper.convert(
                movimiento, actores.get(movimiento.getUsuarioActor().getIdUsuario()))).toList();
    }

    private Usuario buscarUsuarioVigente(Integer idUsuario) {
        Usuario usuario = this.usuarioRepository.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene acceso vigente."));
        if (!List.of("ADMIN", "GESTOR", "LECTOR").contains(usuario.getRol())) {
            throw new AccessDeniedException("El usuario no tiene un rol autorizado.");
        }
        return usuario;
    }

    private boolean esAdmin(Usuario usuario) {
        return "ADMIN".equals(usuario.getRol());
    }

    private String normalizarUbicacion(String ubicacion) {
        return ubicacion == null || ubicacion.isBlank() ? null : ubicacion.trim();
    }

    private ResourceNotFoundException equipoInexistente(Integer idEquipo) {
        return new ResourceNotFoundException("No existe un equipo con el ID " + idEquipo + ".");
    }
}

