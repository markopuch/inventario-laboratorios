package com.utec.inventario.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.utec.inventario.domain.*;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.repository.*;

@Service
@Transactional(readOnly = true)
public class ReporteService {
    private final ReporteRepository repository;
    private final AlcanceLaboratorioService alcanceService;
    private final LaboratorioRepository laboratorios;
    @Autowired
    public ReporteService(ReporteRepository repository, AlcanceLaboratorioService alcanceService,
            LaboratorioRepository laboratorios) {
        this.repository = repository;
        this.alcanceService = alcanceService;
        this.laboratorios = laboratorios;
    }

    public ResumenReporte resumen(Integer actor, FiltroReporte filtro) {
        AlcanceLaboratorios alcance = validar(actor, filtro);
        List<Integer> ids = ids(alcance);
        List<ConteoReporte> equipos = repository.equiposPorEstado(filtro, ids, alcance.isAlcanceGlobal());
        List<ConteoReporte> movimientos = repository.movimientosPorTipo(filtro, ids, alcance.isAlcanceGlobal());
        List<ConteoReporte> mantenimientos = repository.mantenimientosPorEstado(filtro, ids, alcance.isAlcanceGlobal());
        return new ResumenReporte(total(equipos), total(movimientos), total(mantenimientos), equipos, mantenimientos);
    }

    public List<ConteoReporte> equiposPorEstado(Integer actor, FiltroReporte filtro) {
        AlcanceLaboratorios alcance = validar(actor, filtro);
        return repository.equiposPorEstado(filtro, ids(alcance), alcance.isAlcanceGlobal());
    }

    public List<EquiposLaboratorioReporte> equiposPorLaboratorio(Integer actor, FiltroReporte filtro) {
        AlcanceLaboratorios alcance = validar(actor, filtro);
        return repository.equiposPorLaboratorio(filtro, ids(alcance), alcance.isAlcanceGlobal());
    }

    public MovimientosReporte movimientos(Integer actor, FiltroReporte filtro) {
        AlcanceLaboratorios alcance = validar(actor, filtro);
        List<ConteoReporte> tipos = repository.movimientosPorTipo(filtro, ids(alcance), alcance.isAlcanceGlobal());
        return new MovimientosReporte(total(tipos), tipos);
    }

    public MantenimientosReporte mantenimientos(Integer actor, FiltroReporte filtro) {
        AlcanceLaboratorios alcance = validar(actor, filtro);
        List<ConteoReporte> estados = repository.mantenimientosPorEstado(filtro, ids(alcance), alcance.isAlcanceGlobal());
        List<ConteoReporte> tipos = repository.mantenimientosPorTipo(filtro, ids(alcance), alcance.isAlcanceGlobal());
        return new MantenimientosReporte(total(estados), estados, tipos);
    }

    private AlcanceLaboratorios validar(Integer actor, FiltroReporte filtro) {
        AlcanceLaboratorios alcance = alcanceService.obtenerAlcanceEfectivo(actor);
        if (filtro.getIdLaboratorio() != null) {
            if (alcance.isAlcanceGlobal()) {
                if (!laboratorios.existsById(filtro.getIdLaboratorio()))
                    throw new ResourceNotFoundException("No existe el laboratorio solicitado.");
            } else if (!ids(alcance).contains(filtro.getIdLaboratorio())) {
                throw new AccessDeniedException("No tienes acceso a ese laboratorio.");
            }
        }
        return alcance;
    }
    private List<Integer> ids(AlcanceLaboratorios alcance) {
        return alcance.getLaboratorios().stream().map(Laboratorio::getId).toList();
    }
    private long total(List<ConteoReporte> conteos) {
        return conteos.stream().mapToLong(ConteoReporte::total).sum();
    }
}

