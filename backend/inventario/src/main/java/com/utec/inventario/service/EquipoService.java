package com.utec.inventario.service;

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

import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.SubcategoriaEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.repository.EquipoRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.SubcategoriaRepository;
import com.utec.inventario.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlcanceLaboratorioService alcanceService;
    private final EquipoMapper mapper;

    @Autowired
    public EquipoService(EquipoRepository equipoRepository, SubcategoriaRepository subcategoriaRepository,
            LaboratorioRepository laboratorioRepository, UsuarioRepository usuarioRepository,
            AlcanceLaboratorioService alcanceService, EquipoMapper mapper) {
        this.equipoRepository = equipoRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.usuarioRepository = usuarioRepository;
        this.alcanceService = alcanceService;
        this.mapper = mapper;
    }

    public List<Equipo> listarEquipos(Integer idUsuario, EstadoEquipo estado, Integer idLaboratorio,
            Integer idSubcategoria, Boolean requiereMantenimiento) {
        Usuario usuario = this.buscarUsuarioVigente(idUsuario);
        List<EquipoEntity> equipos;
        if (this.esAdmin(usuario)) {
            // ADMIN conserva el historial de equipos, incluso si su laboratorio está inactivo.
            if (idLaboratorio != null && !this.laboratorioRepository.existsById(idLaboratorio)) {
                throw new ResourceNotFoundException("No existe un laboratorio con el ID " + idLaboratorio + ".");
            }
            equipos = this.equipoRepository.findAllFiltrados(
                    estado, idLaboratorio, idSubcategoria, requiereMantenimiento);
        } else {
            if (idLaboratorio != null) {
                this.alcanceService.validarAccesoLaboratorio(idUsuario, idLaboratorio);
            }
            AlcanceLaboratorios alcance = this.alcanceService.obtenerAlcanceEfectivo(idUsuario);
            List<Integer> idsPermitidos = alcance.getLaboratorios().stream().map(Laboratorio::getId).toList();
            if (idsPermitidos.isEmpty()) {
                return List.of();
            }
            equipos = this.equipoRepository.findAllFiltradosEnLaboratorios(
                    estado, idLaboratorio, idSubcategoria, requiereMantenimiento, idsPermitidos);
        }
        return this.convertirEquipos(equipos);
    }

    public Equipo obtenerEquipo(Integer idUsuario, Integer idEquipo) {
        Usuario usuario = this.buscarUsuarioVigente(idUsuario);
        EquipoEntity equipo = this.equipoRepository.findByIdEquipo(idEquipo)
                .orElseThrow(() -> this.equipoInexistente(idEquipo));
        this.validarAlcance(usuario, equipo.getLaboratorio().getIdLaboratorio());
        return this.convertirEquipos(List.of(equipo)).getFirst();
    }

    @Transactional
    public Equipo crearEquipo(Integer idUsuario, Equipo equipo) {
        Usuario usuario = this.bloquearUsuarioParaEscribir(idUsuario);
        this.validarEstadoEditable(equipo.getEstado());
        this.normalizar(equipo);
        equipo.setCodigoInterno(equipo.getCodigoInterno().trim());
        equipo.setId(null);
        equipo.setFechaCreacion(null);
        equipo.setFechaActualizacion(null);

        // Orden común: actor → subcategoría → laboratorio.
        SubcategoriaEntity subcategoria = this.buscarSubcategoriaActiva(equipo.getSubcategoria().getId());
        LaboratorioEntity laboratorio = this.buscarLaboratorioActivo(equipo.getLaboratorio().getId());
        this.validarAlcance(usuario, laboratorio.getIdLaboratorio());
        Usuario responsable = this.buscarResponsable(equipo.getResponsable(), null);
        this.validarDuplicados(equipo, null);

        EquipoEntity nuevo = this.mapper.toEntity(equipo);
        nuevo.setSubcategoria(subcategoria);
        nuevo.setLaboratorio(laboratorio);
        nuevo.setResponsable(this.referenciaResponsable(responsable));
        EquipoEntity guardado = this.equipoRepository.saveAndFlush(nuevo);
        return this.mapper.convert(guardado, responsable);
    }

    @Transactional
    public Equipo actualizarEquipo(Integer idUsuario, Integer idEquipo, Equipo cambios) {
        Usuario usuario = this.bloquearUsuarioParaEscribir(idUsuario);
        EquipoEntity equipo = this.equipoRepository.findForUpdateByIdEquipo(idEquipo)
                .orElseThrow(() -> this.equipoInexistente(idEquipo));
        this.validarAlcance(usuario, equipo.getLaboratorio().getIdLaboratorio());
        this.validarEstadoEditable(equipo.getEstado());
        this.validarEstadoEditable(cambios.getEstado());

        SubcategoriaEntity subcategoria = this.buscarSubcategoriaActiva(cambios.getSubcategoria().getId());
        // No cambia el laboratorio; este bloqueo también se comparte con su baja.
        this.laboratorioRepository.findForUpdateByIdLaboratorio(equipo.getLaboratorio().getIdLaboratorio())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el laboratorio del equipo."));
        Integer idResponsableActual = this.idResponsable(equipo);
        Usuario responsable = this.buscarResponsable(cambios.getResponsable(), idResponsableActual);
        this.normalizar(cambios);
        this.validarDuplicados(cambios, idEquipo);

        EquipoEntity actualizado = this.mapper.copy(equipo, cambios);
        actualizado.setSubcategoria(subcategoria);
        actualizado.setResponsable(this.referenciaResponsable(responsable));
        actualizado.setFechaActualizacion(OffsetDateTime.now(ZoneOffset.UTC));
        EquipoEntity guardado = this.equipoRepository.saveAndFlush(actualizado);
        return this.mapper.convert(guardado, responsable);
    }

    @Transactional
    public void eliminarEquipo(Integer idUsuario, Integer idEquipo) {
        Usuario usuario = this.bloquearUsuarioParaEscribir(idUsuario);
        EquipoEntity equipo = this.equipoRepository.findForUpdateByIdEquipo(idEquipo)
                .orElseThrow(() -> this.equipoInexistente(idEquipo));
        this.validarAlcance(usuario, equipo.getLaboratorio().getIdLaboratorio());
        this.validarEstadoEditable(equipo.getEstado());
        equipo.setEstado(EstadoEquipo.BAJA);
        equipo.setFechaActualizacion(OffsetDateTime.now(ZoneOffset.UTC));
        this.equipoRepository.saveAndFlush(equipo);
    }

    private Usuario buscarUsuarioVigente(Integer idUsuario) {
        Usuario usuario = this.usuarioRepository.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene acceso vigente."));
        if (!List.of("ADMIN", "GESTOR", "LECTOR").contains(usuario.getRol())) {
            throw new AccessDeniedException("El usuario no tiene un rol autorizado.");
        }
        return usuario;
    }

    private Usuario bloquearUsuarioParaEscribir(Integer idUsuario) {
        // FOR SHARE impide reemplazar asignaciones hasta el commit; permite FK al responsable.
        this.usuarioRepository.findIdForShareByIdUsuario(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene acceso vigente."));
        Usuario usuario = this.buscarUsuarioVigente(idUsuario);
        if (!this.esAdmin(usuario) && !"GESTOR".equals(usuario.getRol())) {
            throw new AccessDeniedException("No tienes permiso para modificar equipos.");
        }
        return usuario;
    }

    private boolean esAdmin(Usuario usuario) {
        return "ADMIN".equals(usuario.getRol());
    }

    private void validarAlcance(Usuario usuario, Integer idLaboratorio) {
        if (!this.esAdmin(usuario)) {
            this.alcanceService.validarAccesoLaboratorio(usuario.getId(), idLaboratorio);
        }
    }

    private void validarEstadoEditable(EstadoEquipo estado) {
        if (estado == EstadoEquipo.BAJA) {
            throw new ConflictException("El estado BAJA solo se establece mediante la baja lógica; "
                    + "un equipo dado de baja no puede modificarse ni darse de baja nuevamente.");
        }
    }

    private SubcategoriaEntity buscarSubcategoriaActiva(Integer idSubcategoria) {
        return this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(idSubcategoria)
                .orElseThrow(() -> {
                    if (this.subcategoriaRepository.existsById(idSubcategoria)) {
                        return new ConflictException("La subcategoría seleccionada está inactiva.");
                    }
                    return new ResourceNotFoundException(
                            "No existe una subcategoría con el ID " + idSubcategoria + ".");
                });
    }

    private LaboratorioEntity buscarLaboratorioActivo(Integer idLaboratorio) {
        return this.laboratorioRepository.findForUpdateByIdLaboratorioAndActivoTrue(idLaboratorio)
                .orElseThrow(() -> {
                    if (this.laboratorioRepository.existsById(idLaboratorio)) {
                        return new ConflictException("El laboratorio seleccionado está inactivo.");
                    }
                    return new ResourceNotFoundException(
                            "No existe un laboratorio con el ID " + idLaboratorio + ".");
                });
    }

    private Usuario buscarResponsable(Usuario solicitado, Integer idActual) {
        if (solicitado == null) {
            return null;
        }
        Usuario responsable = this.usuarioRepository.findPublicByIdUsuario(solicitado.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario con el ID " + solicitado.getId() + "."));
        // Conservar al responsable original no equivale a asignar a un usuario inactivo nuevo.
        if (!responsable.isActivo() && !Objects.equals(responsable.getId(), idActual)) {
            throw new ConflictException("El responsable seleccionado está inactivo.");
        }
        return responsable;
    }

    private UsuarioEntity referenciaResponsable(Usuario responsable) {
        return responsable == null ? null : this.usuarioRepository.getReferenceById(responsable.getId());
    }

    private Integer idResponsable(EquipoEntity equipo) {
        return equipo.getResponsable() == null ? null : equipo.getResponsable().getIdUsuario();
    }

    private List<Equipo> convertirEquipos(List<EquipoEntity> entities) {
        List<Integer> idsResponsables = entities.stream().map(this::idResponsable)
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, Usuario> responsables = idsResponsables.isEmpty() ? Map.of()
                : this.usuarioRepository.findPublicByIdUsuarioIn(idsResponsables).stream()
                        .collect(Collectors.toMap(Usuario::getId, usuario -> usuario));
        return entities.stream()
                .map(entity -> {
                    Integer idResponsable = this.idResponsable(entity);
                    Usuario responsable = idResponsable == null ? null : responsables.get(idResponsable);
                    return this.mapper.convert(entity, responsable);
                })
                .toList();
    }

    private void validarDuplicados(Equipo equipo, Integer idActual) {
        if (idActual == null && this.equipoRepository.existsByCodigoInterno(equipo.getCodigoInterno())) {
            throw new ConflictException("Ya existe un equipo con ese código interno.");
        }
        if (equipo.getSerieUtec() != null && (idActual == null
                ? this.equipoRepository.existsBySerieUtec(equipo.getSerieUtec())
                : this.equipoRepository.existsBySerieUtecAndIdEquipoNot(equipo.getSerieUtec(), idActual))) {
            throw new ConflictException("Ya existe un equipo con esa serie UTEC.");
        }
        if (equipo.getNumeroSerie() != null && (idActual == null
                ? this.equipoRepository.existsByNumeroSerie(equipo.getNumeroSerie())
                : this.equipoRepository.existsByNumeroSerieAndIdEquipoNot(equipo.getNumeroSerie(), idActual))) {
            throw new ConflictException("Ya existe un equipo con ese número de serie.");
        }
    }

    private void normalizar(Equipo equipo) {
        equipo.setNombre(equipo.getNombre().trim());
        equipo.setSerieUtec(this.opcional(equipo.getSerieUtec()));
        equipo.setNumeroSerie(this.opcional(equipo.getNumeroSerie()));
        equipo.setMarca(this.opcional(equipo.getMarca()));
        equipo.setModelo(this.opcional(equipo.getModelo()));
        equipo.setOrdenCompra(this.opcional(equipo.getOrdenCompra()));
        equipo.setUbicacionInterna(this.opcional(equipo.getUbicacionInterna()));
        equipo.setComentario(this.opcional(equipo.getComentario()));
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private ResourceNotFoundException equipoInexistente(Integer idEquipo) {
        return new ResourceNotFoundException("No existe un equipo con el ID " + idEquipo + ".");
    }
}
