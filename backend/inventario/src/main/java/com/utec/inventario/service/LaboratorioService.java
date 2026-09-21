package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.entity.AreaEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.repository.AreaRepository;
import com.utec.inventario.repository.LaboratorioRepository;

@Service
@Transactional(readOnly = true)
public class LaboratorioService {

    private final LaboratorioRepository laboratorioRepository;
    private final AreaRepository areaRepository;
    private final LaboratorioMapper mapper;

    @Autowired
    public LaboratorioService(LaboratorioRepository laboratorioRepository,
            AreaRepository areaRepository, LaboratorioMapper mapper) {
        this.laboratorioRepository = laboratorioRepository;
        this.areaRepository = areaRepository;
        this.mapper = mapper;
    }

    public List<Laboratorio> listarLaboratorios() {
        return this.mapper.convert(this.laboratorioRepository.findAllByActivoTrueOrderByIdLaboratorioAsc());
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
        laboratorio.setFechaCreacion(null);

        LaboratorioEntity nuevoLaboratorio = this.mapper.toEntity(laboratorio);
        nuevoLaboratorio.setArea(area);
        LaboratorioEntity laboratorioGuardado = this.laboratorioRepository.saveAndFlush(nuevoLaboratorio);
        return this.mapper.convert(laboratorioGuardado);
    }

    @Transactional
    public Laboratorio actualizarLaboratorio(Integer id, Laboratorio cambios) {
        LaboratorioEntity laboratorio = this.buscarLaboratorioActivoParaModificar(id);
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
        return this.mapper.convert(laboratorioGuardado);
    }

    @Transactional
    public void eliminarLaboratorio(Integer id) {
        LaboratorioEntity laboratorio = this.buscarLaboratorioActivoParaModificar(id);
        laboratorio.setActivo(false);
        this.laboratorioRepository.saveAndFlush(laboratorio);
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
