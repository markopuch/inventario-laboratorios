package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.UsuarioLaboratorioRepository;
import com.utec.inventario.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class AlcanceLaboratorioService {

    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioLaboratorioRepository asignacionRepository;
    private final LaboratorioMapper mapper;

    @Autowired
    public AlcanceLaboratorioService(UsuarioRepository usuarioRepository,
            LaboratorioRepository laboratorioRepository,
            UsuarioLaboratorioRepository asignacionRepository, LaboratorioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.asignacionRepository = asignacionRepository;
        this.mapper = mapper;
    }

    public AlcanceLaboratorios obtenerAlcanceEfectivo(Integer idUsuario) {
        Usuario usuario = this.buscarUsuarioVigente(idUsuario);
        boolean global = "ADMIN".equals(usuario.getRol());
        List<Laboratorio> laboratorios;
        if (global) {
            laboratorios = this.mapper.convert(
                    this.laboratorioRepository.findAllByActivoTrueOrderByIdLaboratorioAsc());
        } else {
            laboratorios = this.asignacionRepository
                    .findAllByUsuario_IdUsuarioAndActivoTrueAndLaboratorio_ActivoTrueOrderByLaboratorio_IdLaboratorioAsc(
                            idUsuario)
                    .stream()
                    .map(asignacion -> this.mapper.convert(asignacion.getLaboratorio()))
                    .toList();
        }
        return AlcanceLaboratorios.builder()
                .alcanceGlobal(global)
                .laboratorios(laboratorios)
                .build();
    }

    public boolean tieneAccesoLaboratorio(Integer idUsuario, Integer idLaboratorio) {
        Usuario usuario = this.buscarUsuarioVigente(idUsuario);
        if (idLaboratorio == null || idLaboratorio <= 0) {
            return false;
        }
        if ("ADMIN".equals(usuario.getRol())) {
            return this.laboratorioRepository.existsByIdLaboratorioAndActivoTrue(idLaboratorio);
        }
        return this.asignacionRepository
                .existsByUsuario_IdUsuarioAndLaboratorio_IdLaboratorioAndActivoTrueAndLaboratorio_ActivoTrue(
                        idUsuario, idLaboratorio);
    }

    public void validarAccesoLaboratorio(Integer idUsuario, Integer idLaboratorio) {
        if (!this.tieneAccesoLaboratorio(idUsuario, idLaboratorio)) {
            throw new AccessDeniedException("No tienes acceso a ese laboratorio.");
        }
    }

    private Usuario buscarUsuarioVigente(Integer idUsuario) {
        // Permite reutilizar el servicio sin confiar en un rol enviado por un cliente.
        Usuario usuario = this.usuarioRepository.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene acceso vigente."));
        if (!List.of("ADMIN", "GESTOR", "LECTOR").contains(usuario.getRol())) {
            throw new AccessDeniedException("El usuario no tiene un rol autorizado.");
        }
        return usuario;
    }
}

