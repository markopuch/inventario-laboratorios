package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Area;
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

    private final AreaRepository areaRepository;
    private final SedeRepository sedeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final AreaMapper mapper;

    @Autowired
    public AreaService(AreaRepository areaRepository, SedeRepository sedeRepository,
            LaboratorioRepository laboratorioRepository, AreaMapper mapper) {
        this.areaRepository = areaRepository;
        this.sedeRepository = sedeRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.mapper = mapper;
    }

    public List<Area> listarAreas() {
        return this.mapper.convert(this.areaRepository.findAllByActivoTrueOrderByIdAreaAsc());
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
        return this.mapper.convert(areaGuardada);
    }

    @Transactional
    public void eliminarArea(Integer id) {
        AreaEntity area = this.buscarAreaActivaParaModificar(id);
        // Crear o mover un Laboratorio a esta Area necesita el mismo bloqueo del padre.
        if (this.laboratorioRepository.existsByArea_IdAreaAndActivoTrue(id)) {
            throw new ConflictException("No se puede desactivar el área porque contiene laboratorios activos.");
        }
        area.setActivo(false);
        this.areaRepository.saveAndFlush(area);
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
