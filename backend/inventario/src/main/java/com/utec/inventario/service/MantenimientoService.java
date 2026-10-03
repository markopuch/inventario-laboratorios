package com.utec.inventario.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.AccionAuditoria;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.EstadoMantenimiento;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Mantenimiento;
import com.utec.inventario.domain.TipoMantenimiento;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.MantenimientoEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.MantenimientoMapper;
import com.utec.inventario.repository.EquipoRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.MantenimientoRepository;
import com.utec.inventario.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class MantenimientoService {
    private final MantenimientoRepository mantenimientoRepository;
    private final EquipoRepository equipoRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlcanceLaboratorioService alcanceService;
    private final MantenimientoMapper mapper;
    private final AuditoriaService auditoriaService;

    @Autowired
    public MantenimientoService(MantenimientoRepository mantenimientoRepository, EquipoRepository equipoRepository,
            LaboratorioRepository laboratorioRepository, UsuarioRepository usuarioRepository,
            AlcanceLaboratorioService alcanceService, MantenimientoMapper mapper, AuditoriaService auditoriaService) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.equipoRepository = equipoRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.usuarioRepository = usuarioRepository;
        this.alcanceService = alcanceService;
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
    }

    public List<Mantenimiento> listarMantenimientos(Integer idUsuario, Integer idEquipo, Integer idLaboratorio,
            EstadoMantenimiento estado, TipoMantenimiento tipo, LocalDate fechaDesde, LocalDate fechaHasta) {
        Usuario actor = this.buscarActor(idUsuario);
        if (idEquipo != null) {
            EquipoEntity equipo = this.equipoRepository.findByIdEquipo(idEquipo)
                    .orElseThrow(() -> this.equipoInexistente(idEquipo));
            this.validarAlcance(actor, equipo);
        }
        if (idLaboratorio != null) {
            if (this.esAdmin(actor)) {
                if (!this.laboratorioRepository.existsById(idLaboratorio)) {
                    throw new ResourceNotFoundException("No existe el laboratorio indicado.");
                }
            } else {
                this.alcanceService.validarAccesoLaboratorio(idUsuario, idLaboratorio);
            }
        }
        List<Integer> permitidos = null;
        if (!this.esAdmin(actor)) {
            permitidos = this.alcanceService.obtenerAlcanceEfectivo(idUsuario).getLaboratorios().stream()
                    .map(Laboratorio::getId).toList();
            if (permitidos.isEmpty()) return List.of();
        }
        return this.convertir(this.mantenimientoRepository.findAllFiltrados(
                idEquipo, idLaboratorio, estado, tipo, fechaDesde, fechaHasta, permitidos));
    }

    public Mantenimiento obtenerMantenimiento(Integer idUsuario, Integer idMantenimiento) {
        Usuario actor = this.buscarActor(idUsuario);
        MantenimientoEntity entity = this.mantenimientoRepository.findByIdMantenimiento(idMantenimiento)
                .orElseThrow(() -> this.mantenimientoInexistente(idMantenimiento));
        this.validarAlcance(actor, entity.getEquipo());
        return this.convertir(List.of(entity)).getFirst();
    }

    @Transactional
    public Mantenimiento crearMantenimiento(Integer idUsuario, Mantenimiento datos) {
        Usuario actor = this.bloquearActor(idUsuario);
        EquipoEntity equipo = this.bloquearEquipo(datos.getEquipo().getId());
        this.validarAlcance(actor, equipo);
        this.validarEquipoVigente(equipo);
        this.validarLaboratorioActivo(equipo);
        Usuario responsable = this.buscarResponsable(datos.getResponsable(), null);
        this.normalizar(datos);

        MantenimientoEntity nuevo = this.mapper.toEntity(datos);
        nuevo.setIdMantenimiento(null);
        nuevo.setEquipo(equipo);
        nuevo.setEstado(EstadoMantenimiento.PROGRAMADO);
        nuevo.setFechaInicio(null);
        nuevo.setFechaFin(null);
        nuevo.setEstadoEquipoAnterior(null);
        nuevo.setFechaCreacion(null);
        nuevo.setFechaActualizacion(null);
        nuevo.setResponsable(this.referenciaResponsable(responsable));
        MantenimientoEntity guardado = this.mantenimientoRepository.saveAndFlush(nuevo);
        this.auditoriaService.registrar(AccionAuditoria.CREAR, "mantenimiento", guardado.getIdMantenimiento(),
                this.resumen(guardado, null));
        return this.mapper.convert(guardado, responsable);
    }

    @Transactional
    public Mantenimiento actualizarMantenimiento(Integer idUsuario, Integer idMantenimiento, Mantenimiento datos) {
        Usuario actor = this.bloquearActor(idUsuario);
        MantenimientoEntity entity = this.bloquearMantenimiento(idMantenimiento);
        this.validarAlcance(actor, entity.getEquipo());
        this.validarEquipoVigente(entity.getEquipo());
        if (entity.getEstado() != EstadoMantenimiento.PROGRAMADO) {
            throw new ConflictException("Solo puede editarse un mantenimiento PROGRAMADO.");
        }
        this.validarLaboratorioActivo(entity.getEquipo());
        Usuario responsable = this.buscarResponsable(datos.getResponsable(), this.idResponsable(entity));
        this.normalizar(datos);
        entity.setTipo(datos.getTipo());
        entity.setDescripcion(datos.getDescripcion());
        entity.setFechaProgramada(datos.getFechaProgramada());
        entity.setObservaciones(datos.getObservaciones());
        entity.setResponsable(this.referenciaResponsable(responsable));
        entity.setFechaActualizacion(this.ahora());
        MantenimientoEntity guardado = this.mantenimientoRepository.saveAndFlush(entity);
        this.auditoriaService.registrar(AccionAuditoria.EDITAR, "mantenimiento", guardado.getIdMantenimiento(),
                this.resumen(guardado, EstadoMantenimiento.PROGRAMADO));
        return this.mapper.convert(guardado, responsable);
    }

    @Transactional
    public Mantenimiento cambiarEstado(Integer idUsuario, Integer idMantenimiento,
            EstadoMantenimiento destino, String observaciones) {
        Usuario actor = this.bloquearActor(idUsuario);
        MantenimientoEntity entity = this.bloquearMantenimiento(idMantenimiento);
        EquipoEntity equipo = entity.getEquipo();
        this.validarAlcance(actor, equipo);
        EstadoMantenimiento origen = entity.getEstado();
        this.validarTransicion(origen, destino);
        OffsetDateTime ahora = this.ahora();
        AccionAuditoria accion;

        if (destino == EstadoMantenimiento.EN_PROCESO) {
            this.validarEquipoVigente(equipo);
            this.validarLaboratorioActivo(equipo);
            if (this.mantenimientoRepository.existsByEquipo_IdEquipoAndEstado(
                    equipo.getIdEquipo(), EstadoMantenimiento.EN_PROCESO)) {
                throw new ConflictException("El equipo ya tiene un mantenimiento EN_PROCESO.");
            }
            entity.setEstadoEquipoAnterior(equipo.getEstado());
            entity.setFechaInicio(ahora);
            // Flush de Equipo también puede enviar Mantenimiento; su ciclo debe ser coherente ya.
            entity.setEstado(destino);
            equipo.setEstado(EstadoEquipo.MANTENIMIENTO);
            equipo.setFechaActualizacion(ahora);
            this.equipoRepository.saveAndFlush(equipo);
            accion = AccionAuditoria.INICIAR_MANTENIMIENTO;
        } else {
            entity.setFechaFin(ahora);
            entity.setEstado(destino);
            if (origen == EstadoMantenimiento.EN_PROCESO) {
                if (equipo.getEstado() != EstadoEquipo.MANTENIMIENTO || entity.getEstadoEquipoAnterior() == null) {
                    throw new ConflictException("El estado del equipo no coincide con su mantenimiento activo.");
                }
                equipo.setEstado(entity.getEstadoEquipoAnterior());
                equipo.setFechaActualizacion(ahora);
                this.equipoRepository.saveAndFlush(equipo);
            }
            accion = destino == EstadoMantenimiento.COMPLETADO
                    ? AccionAuditoria.COMPLETAR_MANTENIMIENTO : AccionAuditoria.CANCELAR_MANTENIMIENTO;
        }
        entity.setEstado(destino);
        if (observaciones != null) entity.setObservaciones(this.opcional(observaciones));
        entity.setFechaActualizacion(ahora);
        MantenimientoEntity guardado = this.mantenimientoRepository.saveAndFlush(entity);
        this.auditoriaService.registrar(accion, "mantenimiento", guardado.getIdMantenimiento(), this.resumen(guardado, origen));
        return this.convertir(List.of(guardado)).getFirst();
    }

    private MantenimientoEntity bloquearMantenimiento(Integer id) {
        Integer idEquipo = this.mantenimientoRepository.findIdEquipoByIdMantenimiento(id)
                .orElseThrow(() -> this.mantenimientoInexistente(id));
        EquipoEntity equipo = this.bloquearEquipo(idEquipo);
        MantenimientoEntity mantenimiento = this.mantenimientoRepository.findForUpdateByIdMantenimiento(id)
                .orElseThrow(() -> this.mantenimientoInexistente(id));
        mantenimiento.setEquipo(equipo);
        return mantenimiento;
    }

    private EquipoEntity bloquearEquipo(Integer id) {
        return this.equipoRepository.findForUpdateByIdEquipo(id).orElseThrow(() -> this.equipoInexistente(id));
    }

    private void validarTransicion(EstadoMantenimiento origen, EstadoMantenimiento destino) {
        boolean valida = origen == EstadoMantenimiento.PROGRAMADO
                && (destino == EstadoMantenimiento.EN_PROCESO || destino == EstadoMantenimiento.CANCELADO)
                || origen == EstadoMantenimiento.EN_PROCESO
                && (destino == EstadoMantenimiento.COMPLETADO || destino == EstadoMantenimiento.CANCELADO);
        if (!valida) throw new ConflictException("Transición de mantenimiento no permitida: " + origen + " -> " + destino + ".");
    }

    private void validarEquipoVigente(EquipoEntity equipo) {
        if (equipo.getEstado() == EstadoEquipo.BAJA) {
            throw new ConflictException("Un equipo en BAJA no puede recibir ni iniciar mantenimiento.");
        }
    }

    private void validarLaboratorioActivo(EquipoEntity equipo) {
        var laboratorio = this.laboratorioRepository.findForUpdateByIdLaboratorio(equipo.getLaboratorio().getIdLaboratorio())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el laboratorio del equipo."));
        if (!laboratorio.isActivo()) throw new ConflictException("El laboratorio del equipo está inactivo.");
    }

    private Usuario buscarActor(Integer idUsuario) {
        Usuario usuario = this.usuarioRepository.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene acceso vigente."));
        if (!List.of("ADMIN", "GESTOR", "LECTOR").contains(usuario.getRol())) {
            throw new AccessDeniedException("El usuario no tiene un rol autorizado.");
        }
        return usuario;
    }

    private Usuario bloquearActor(Integer idUsuario) {
        this.usuarioRepository.findIdForShareByIdUsuario(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene acceso vigente."));
        Usuario actor = this.buscarActor(idUsuario);
        if (!this.esAdmin(actor) && !"GESTOR".equals(actor.getRol())) {
            throw new AccessDeniedException("No tienes permiso para modificar mantenimientos.");
        }
        return actor;
    }

    private boolean esAdmin(Usuario actor) { return "ADMIN".equals(actor.getRol()); }

    private void validarAlcance(Usuario actor, EquipoEntity equipo) {
        if (!this.esAdmin(actor)) this.alcanceService.validarAccesoLaboratorio(actor.getId(), equipo.getLaboratorio().getIdLaboratorio());
    }

    private Usuario buscarResponsable(Usuario solicitado, Integer idActual) {
        if (solicitado == null) return null;
        this.usuarioRepository.findIdForShareByIdUsuario(solicitado.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el responsable indicado."));
        Usuario responsable = this.usuarioRepository.findPublicByIdUsuario(solicitado.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el responsable indicado."));
        if (!responsable.isActivo() && !Objects.equals(responsable.getId(), idActual)) {
            throw new ConflictException("El responsable seleccionado está inactivo.");
        }
        return responsable;
    }

    private UsuarioEntity referenciaResponsable(Usuario responsable) {
        return responsable == null ? null : this.usuarioRepository.getReferenceById(responsable.getId());
    }

    private Integer idResponsable(MantenimientoEntity entity) {
        return entity.getResponsable() == null ? null : entity.getResponsable().getIdUsuario();
    }

    private List<Mantenimiento> convertir(List<MantenimientoEntity> entities) {
        List<Integer> ids = entities.stream().map(this::idResponsable).filter(Objects::nonNull).distinct().toList();
        Map<Integer, Usuario> responsables = ids.isEmpty() ? Map.of() : this.usuarioRepository.findPublicByIdUsuarioIn(ids)
                .stream().collect(Collectors.toMap(Usuario::getId, usuario -> usuario));
        return entities.stream().map(entity -> {
            Integer idResponsable = this.idResponsable(entity);
            return this.mapper.convert(entity, idResponsable == null ? null : responsables.get(idResponsable));
        }).toList();
    }

    private void normalizar(Mantenimiento datos) {
        datos.setDescripcion(datos.getDescripcion().trim());
        datos.setObservaciones(this.opcional(datos.getObservaciones()));
    }

    private String opcional(String texto) { return texto == null || texto.isBlank() ? null : texto.trim(); }
    private OffsetDateTime ahora() { return OffsetDateTime.now(ZoneOffset.UTC); }
    private String resumen(MantenimientoEntity entity, EstadoMantenimiento anterior) {
        return "equipo=" + entity.getEquipo().getIdEquipo() + "; tipo=" + entity.getTipo()
                + "; fechaProgramada=" + entity.getFechaProgramada() + "; estado=" + anterior + " -> " + entity.getEstado();
    }
    private ResourceNotFoundException equipoInexistente(Integer id) { return new ResourceNotFoundException("No existe un equipo con el ID " + id + "."); }
    private ResourceNotFoundException mantenimientoInexistente(Integer id) { return new ResourceNotFoundException("No existe un mantenimiento con el ID " + id + "."); }
}
