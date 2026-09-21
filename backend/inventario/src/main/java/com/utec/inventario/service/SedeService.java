package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Sede;
import com.utec.inventario.entity.SedeEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.SedeMapper;
import com.utec.inventario.repository.AreaRepository;
import com.utec.inventario.repository.SedeRepository;

@Service
@Transactional(readOnly = true)
public class SedeService {

    private final SedeRepository sedeRepository;
    private final AreaRepository areaRepository;
    private final SedeMapper mapper;

    @Autowired
    public SedeService(SedeRepository sedeRepository, AreaRepository areaRepository, SedeMapper mapper) {
        this.sedeRepository = sedeRepository;
        this.areaRepository = areaRepository;
        this.mapper = mapper;
    }

    public List<Sede> listarSedes() {
        return this.mapper.convert(this.sedeRepository.findAllByActivoTrueOrderByIdSedeAsc());
    }

    public Sede obtenerSede(Integer id) {
        SedeEntity sede = this.sedeRepository.findByIdSedeAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una sede activa con el ID " + id + "."));
        return this.mapper.convert(sede);
    }

    @Transactional
    public Sede crearSede(Sede sede) {
        this.normalizar(sede);
        sede.setId(null);
        sede.setActivo(true);
        sede.setFechaCreacion(null);

        SedeEntity nuevaSede = this.mapper.toEntity(sede);
        SedeEntity sedeGuardada = this.sedeRepository.saveAndFlush(nuevaSede);
        return this.mapper.convert(sedeGuardada);
    }

    @Transactional
    public Sede actualizarSede(Integer id, Sede cambios) {
        SedeEntity sede = this.buscarSedeActivaParaModificar(id);
        this.normalizar(cambios);

        SedeEntity sedeActualizada = this.mapper.copy(sede, cambios);
        SedeEntity sedeGuardada = this.sedeRepository.saveAndFlush(sedeActualizada);
        return this.mapper.convert(sedeGuardada);
    }

    @Transactional
    public void eliminarSede(Integer id) {
        SedeEntity sede = this.buscarSedeActivaParaModificar(id);
        // Crear o mover un Area a esta Sede necesita el mismo bloqueo del padre.
        if (this.areaRepository.existsBySede_IdSedeAndActivoTrue(id)) {
            throw new ConflictException("No se puede desactivar la sede porque contiene áreas activas.");
        }
        sede.setActivo(false);
        this.sedeRepository.saveAndFlush(sede);
    }

    private SedeEntity buscarSedeActivaParaModificar(Integer id) {
        return this.sedeRepository.findForUpdateByIdSedeAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una sede activa con el ID " + id + "."));
    }

    private void normalizar(Sede sede) {
        sede.setNombre(sede.getNombre().trim());
        sede.setDireccion(this.normalizarOpcional(sede.getDireccion()));
        sede.setDistrito(this.normalizarOpcional(sede.getDistrito()));
        sede.setDepartamento(this.normalizarOpcional(sede.getDepartamento()));
    }

    private String normalizarOpcional(String valor) {
        return valor == null ? null : valor.trim();
    }
}
