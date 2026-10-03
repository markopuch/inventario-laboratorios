package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Area;
import com.utec.inventario.domain.AccionAuditoria;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.SedeEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.AreaMapper;
import com.utec.inventario.repository.AreaRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.SedeRepository;

@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AuditoriaService auditoriaService;

    private final AreaRepository areaRepository;
    private final SedeRepository sedeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final AreaMapper mapper;

    @Autowired
    public AreaService(AreaRepository areaRepository, SedeRepository sedeRepository,
            LaboratorioRepository laboratorioRepository, AreaMapper mapper, AuditoriaService auditoriaService) {
        this.areaRepository = areaRepository;
        this.sedeRepository = sedeRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
    }

    public List<Area> listarAreas() {
        return this.mapper.convert(this.areaRepository.findAllByActivoTrueOrderByIdAreaAsc());
    }

    public List<Area> listarAreasAdmin(Boolean activo) {
        return this.mapper.convert(activo == null
                ? this.areaRepository.findAllByOrderByIdAreaAsc()
                : this.areaRepository.findAllByActivoOrderByIdAreaAsc(activo));
    }

    @Transactional
    public Area cambiarEstadoArea(Integer id, boolean activo) {
        AreaEntity area = this.areaRepository.findForUpdateByIdArea(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un área con el ID " + id + "."));
        if (area.isActivo() == activo) {
            return this.mapper.convert(area);
        }
        if (activo) {
            this.buscarSedeActivaParaRelacionar(area.getSede().getIdSede());
        } else {
            this.validarDesactivacion(id);
        }
        area.setActivo(activo);
        AreaEntity guardado = this.areaRepository.saveAndFlush(area);
        this.auditoriaService.registrar(activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR,
                "area", id, "activo: " + !activo + " -> " + activo);
        return this.mapper.convert(guardado);
    }

    public Area obtenerArea(Integer id) {
        AreaEntity area = this.areaRepository.findByIdAreaAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un área activa con el ID " + id + "."));
        return this.mapper.convert(area);
    }

    public List<Area> listarPorSede(Integer idSede) {
        this.sedeRepository.findByIdSedeAndActivoTrue(idSede)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una sede activa con el ID " + idSede + "."));
        return this.mapper.convert(this.areaRepository
                .findAllBySede_IdSedeAndActivoTrueOrderByIdAreaAsc(idSede));
    }

    @Transactional
    public Area crearArea(Area area) {
        SedeEntity sede = this.buscarSedeActivaParaRelacionar(area.getSede().getId());
        String nombre = area.getNombre().trim();
        if (this.areaRepository.existsBySede_IdSedeAndNombreIgnoreCase(sede.getIdSede(), nombre)) {
            throw new ConflictException("Ya existe un área con ese nombre en la sede seleccionada.");
        }

        area.setId(null);
        area.setNombre(nombre);
        area.setDescripcion(this.normalizarDescripcion(area.getDescripcion()));
        area.setActivo(true);
        area.setFechaCreacion(null);

        AreaEntity nuevaArea = this.mapper.toEntity(area);
        nuevaArea.setSede(sede);
        AreaEntity areaGuardada = this.areaRepository.saveAndFlush(nuevaArea);
        this.auditoriaService.registrar(AccionAuditoria.CREAR, "area", areaGuardada.getIdArea(),
                "Creación de catálogo; activo: true");
        return this.mapper.convert(areaGuardada);
    }

    @Transactional
    public Area actualizarArea(Integer id, Area cambios) {
        AreaEntity area = this.buscarAreaActivaParaModificar(id);
        SedeEntity sede = this.buscarSedeActivaParaRelacionar(cambios.getSede().getId());
        String nombre = cambios.getNombre().trim();
        if (this.areaRepository.existsBySede_IdSedeAndNombreIgnoreCaseAndIdAreaNot(
                sede.getIdSede(), nombre, id)) {
            throw new ConflictException("Ya existe un área con ese nombre en la sede seleccionada.");
        }

        cambios.setNombre(nombre);
        cambios.setDescripcion(this.normalizarDescripcion(cambios.getDescripcion()));

        AreaEntity areaActualizada = this.mapper.copy(area, cambios);
        areaActualizada.setSede(sede);
        AreaEntity areaGuardada = this.areaRepository.saveAndFlush(areaActualizada);
        this.auditoriaService.registrar(AccionAuditoria.EDITAR, "area", id,
                "Actualización de nombre, descripcion, idSede: " + sede.getIdSede());
        return this.mapper.convert(areaGuardada);
    }

    @Transactional
    public void eliminarArea(Integer id) {
        AreaEntity area = this.buscarAreaActivaParaModificar(id);
        this.validarDesactivacion(id);
        area.setActivo(false);
        this.areaRepository.saveAndFlush(area);
        this.auditoriaService.registrar(AccionAuditoria.DESACTIVAR, "area", id, "activo: true -> false");
    }

    private void validarDesactivacion(Integer id) {
        // Crear o mover un Laboratorio a esta Area necesita el mismo bloqueo del padre.
        if (this.laboratorioRepository.existsByArea_IdAreaAndActivoTrue(id)) {
            throw new ConflictException("No se puede desactivar el área porque contiene laboratorios activos.");
        }
    }

    private AreaEntity buscarAreaActivaParaModificar(Integer id) {
        return this.areaRepository.findForUpdateByIdAreaAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un área activa con el ID " + id + "."));
    }

    private SedeEntity buscarSedeActivaParaRelacionar(Integer idSede) {
        // Comparte el bloqueo usado por DELETE de Sede hasta terminar la transacción.
        return this.sedeRepository.findForUpdateByIdSedeAndActivoTrue(idSede)
                .orElseThrow(() -> {
                    if (this.sedeRepository.existsById(idSede)) {
                        return new ConflictException("La sede seleccionada está inactiva.");
                    }
                    return new ResourceNotFoundException("No existe una sede con el ID " + idSede + ".");
                });
    }

    private String normalizarDescripcion(String descripcion) {
        return descripcion == null ? null : descripcion.trim();
    }
}
