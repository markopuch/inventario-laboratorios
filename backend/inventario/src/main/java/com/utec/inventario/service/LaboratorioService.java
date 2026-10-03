package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.AccionAuditoria;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.EstadoOperativoLaboratorio;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.repository.AreaRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.UsuarioLaboratorioRepository;
import com.utec.inventario.repository.EquipoRepository;

@Service
@Transactional(readOnly = true)
public class LaboratorioService {

    private final AuditoriaService auditoriaService;

    private final LaboratorioRepository laboratorioRepository;
    private final AreaRepository areaRepository;
    private final LaboratorioMapper mapper;
    private final UsuarioLaboratorioRepository usuarioLaboratorioRepository;
    private final EquipoRepository equipoRepository;

    @Autowired
    public LaboratorioService(LaboratorioRepository laboratorioRepository,
            AreaRepository areaRepository, LaboratorioMapper mapper,
            UsuarioLaboratorioRepository usuarioLaboratorioRepository, EquipoRepository equipoRepository, AuditoriaService auditoriaService) {
        this.laboratorioRepository = laboratorioRepository;
        this.areaRepository = areaRepository;
        this.mapper = mapper;
        this.usuarioLaboratorioRepository = usuarioLaboratorioRepository;
        this.equipoRepository = equipoRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Laboratorio> listarLaboratorios() {
        return this.mapper.convert(this.laboratorioRepository.findAllByActivoTrueOrderByIdLaboratorioAsc());
    }

    public List<Laboratorio> listarLaboratoriosAdmin(Boolean activo) {
        return this.mapper.convert(activo == null
                ? this.laboratorioRepository.findAllByOrderByIdLaboratorioAsc()
                : this.laboratorioRepository.findAllByActivoOrderByIdLaboratorioAsc(activo));
    }

    @Transactional
    public Laboratorio cambiarEstadoLaboratorio(Integer id, boolean activo) {
        LaboratorioEntity laboratorio = this.laboratorioRepository.findForUpdateByIdLaboratorio(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un laboratorio con el ID " + id + "."));
        if (laboratorio.isActivo() == activo) {
            return this.mapper.convert(laboratorio);
        }
        if (activo) {
            this.buscarAreaActivaParaRelacionar(laboratorio.getArea().getIdArea());
        } else {
            this.validarDesactivacion(id);
        }
        laboratorio.setActivo(activo);
        LaboratorioEntity guardado = this.laboratorioRepository.saveAndFlush(laboratorio);
        this.auditoriaService.registrar(activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR,
                "laboratorio", id, "activo: " + !activo + " -> " + activo);
        return this.mapper.convert(guardado);
    }

    public Laboratorio obtenerLaboratorio(Integer id) {
        LaboratorioEntity laboratorio = this.laboratorioRepository.findByIdLaboratorioAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un laboratorio activo con el ID " + id + "."));
        return this.mapper.convert(laboratorio);
    }

    public List<Laboratorio> listarPorArea(Integer idArea) {
        this.areaRepository.findByIdAreaAndActivoTrue(idArea)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un área activa con el ID " + idArea + "."));
        return this.mapper.convert(this.laboratorioRepository
                .findAllByArea_IdAreaAndActivoTrueOrderByIdLaboratorioAsc(idArea));
    }

    @Transactional
    public Laboratorio crearLaboratorio(Laboratorio laboratorio) {
        AreaEntity area = this.buscarAreaActivaParaRelacionar(laboratorio.getArea().getId());
        String codigo = laboratorio.getCodigo().trim();
        if (this.laboratorioRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un laboratorio con ese código.");
        }

        laboratorio.setId(null);
        laboratorio.setNombre(laboratorio.getNombre().trim());
        laboratorio.setCodigo(codigo);
        laboratorio.setUbicacion(this.normalizarUbicacion(laboratorio.getUbicacion()));
        laboratorio.setActivo(true);
        if (laboratorio.getEstadoOperativo() == null) {
            laboratorio.setEstadoOperativo(EstadoOperativoLaboratorio.OPERATIVO);
        }
        laboratorio.setFechaCreacion(null);

        LaboratorioEntity nuevoLaboratorio = this.mapper.toEntity(laboratorio);
        nuevoLaboratorio.setArea(area);
        LaboratorioEntity laboratorioGuardado = this.laboratorioRepository.saveAndFlush(nuevoLaboratorio);
        this.auditoriaService.registrar(AccionAuditoria.CREAR, "laboratorio", laboratorioGuardado.getIdLaboratorio(),
                "Creación de catálogo; activo: true");
        return this.mapper.convert(laboratorioGuardado);
    }

    @Transactional
    public Laboratorio actualizarLaboratorio(Integer id, Laboratorio cambios) {
        LaboratorioEntity laboratorio = this.buscarLaboratorioActivoParaModificar(id);
        EstadoOperativoLaboratorio estadoAnterior = laboratorio.getEstadoOperativo();
        AreaEntity area = this.buscarAreaActivaParaRelacionar(cambios.getArea().getId());
        String codigo = cambios.getCodigo().trim();
        if (this.laboratorioRepository.existsByCodigoIgnoreCaseAndIdLaboratorioNot(codigo, id)) {
            throw new ConflictException("Ya existe un laboratorio con ese código.");
        }

        cambios.setNombre(cambios.getNombre().trim());
        cambios.setCodigo(codigo);
        cambios.setUbicacion(this.normalizarUbicacion(cambios.getUbicacion()));

        LaboratorioEntity laboratorioActualizado = this.mapper.copy(laboratorio, cambios);
        laboratorioActualizado.setArea(area);
        LaboratorioEntity laboratorioGuardado = this.laboratorioRepository.saveAndFlush(laboratorioActualizado);
        this.auditoriaService.registrar(AccionAuditoria.EDITAR, "laboratorio", id,
                "Actualización de nombre, codigo, ubicacion, idArea: " + area.getIdArea()
                        + "; estadoOperativo: " + estadoAnterior + " -> " + laboratorioGuardado.getEstadoOperativo());
        return this.mapper.convert(laboratorioGuardado);
    }

    @Transactional
    public void eliminarLaboratorio(Integer id) {
        LaboratorioEntity laboratorio = this.buscarLaboratorioActivoParaModificar(id);
        this.validarDesactivacion(id);
        laboratorio.setActivo(false);
        this.laboratorioRepository.saveAndFlush(laboratorio);
        this.auditoriaService.registrar(AccionAuditoria.DESACTIVAR, "laboratorio", id, "activo: true -> false");
    }

    private void validarDesactivacion(Integer id) {
        // Comparte el bloqueo con PUT de asignaciones; no depende de usuario.activo.
        if (this.usuarioLaboratorioRepository.existsByLaboratorio_IdLaboratorioAndActivoTrue(id)) {
            throw new ConflictException(
                    "No se puede desactivar el laboratorio porque tiene asignaciones activas de usuarios.");
        }
        if (this.equipoRepository.existsByLaboratorio_IdLaboratorioAndEstadoNot(id, EstadoEquipo.BAJA)) {
            throw new ConflictException(
                    "No se puede desactivar el laboratorio porque contiene equipos no dados de baja.");
        }
    }

    private LaboratorioEntity buscarLaboratorioActivoParaModificar(Integer id) {
        return this.laboratorioRepository.findForUpdateByIdLaboratorioAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un laboratorio activo con el ID " + id + "."));
    }

    private AreaEntity buscarAreaActivaParaRelacionar(Integer idArea) {
        // Comparte el bloqueo usado por DELETE de Area hasta terminar la transacción.
        return this.areaRepository.findForUpdateByIdAreaAndActivoTrue(idArea)
                .orElseThrow(() -> {
                    if (this.areaRepository.existsById(idArea)) {
                        return new ConflictException("El área seleccionada está inactiva.");
                    }
                    return new ResourceNotFoundException("No existe un área con el ID " + idArea + ".");
                });
    }

    private String normalizarUbicacion(String ubicacion) {
        return ubicacion == null ? null : ubicacion.trim();
    }
}
