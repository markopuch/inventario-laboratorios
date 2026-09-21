package com.utec.inventario.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.domain.UsuarioLaboratorio;
import com.utec.inventario.domain.UsuarioLaboratorios;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioId;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.UsuarioLaboratorioMapper;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.UsuarioLaboratorioRepository;
import com.utec.inventario.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioLaboratorioService {

    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioLaboratorioRepository asignacionRepository;
    private final UsuarioLaboratorioMapper mapper;

    @Autowired
    public UsuarioLaboratorioService(UsuarioRepository usuarioRepository,
            LaboratorioRepository laboratorioRepository,
            UsuarioLaboratorioRepository asignacionRepository,
            UsuarioLaboratorioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.asignacionRepository = asignacionRepository;
        this.mapper = mapper;
    }

    public UsuarioLaboratorios listarAsignacionesActivas(Integer idUsuario) {
        Usuario usuario = this.buscarUsuarioPublico(idUsuario);
        return this.obtenerAsignacionesActivas(usuario);
    }

    @Transactional
    public UsuarioLaboratorios reemplazarAsignaciones(Integer idUsuario, List<Integer> idsLaboratorio) {
        // Siempre primero Usuario, aunque esté inactivo: serializa sus PUT completos.
        this.usuarioRepository.findIdForUpdateByIdUsuario(idUsuario)
                .orElseThrow(() -> this.usuarioInexistente(idUsuario));
        Usuario usuario = this.buscarUsuarioPublico(idUsuario);

        Set<Integer> idsDestino = new TreeSet<>(idsLaboratorio);
        Map<Integer, LaboratorioEntity> destinos = new LinkedHashMap<>();
        for (Integer idLaboratorio : idsDestino) {
            // Orden ascendente compartido por todas las operaciones de asignación.
            destinos.put(idLaboratorio, this.buscarLaboratorioActivoParaAsignar(idLaboratorio));
        }

        // Se validó todo el destino antes de modificar una sola asignación.
        List<UsuarioLaboratorioEntity> existentes = this.asignacionRepository
                .findAllByUsuario_IdUsuarioOrderByLaboratorio_IdLaboratorioAsc(idUsuario);
        Map<Integer, UsuarioLaboratorioEntity> porLaboratorio = existentes.stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getId().getIdLaboratorio(),
                        asignacion -> asignacion));
        List<UsuarioLaboratorioEntity> cambios = new ArrayList<>();
        for (UsuarioLaboratorioEntity asignacion : existentes) {
            boolean activo = idsDestino.contains(asignacion.getId().getIdLaboratorio());
            if (asignacion.isActivo() != activo) {
                asignacion.setActivo(activo);
                cambios.add(asignacion);
            }
        }

        // getReferenceById proporciona la FK sin consultar los datos sensibles del usuario.
        UsuarioEntity referenciaUsuario = null;
        for (Map.Entry<Integer, LaboratorioEntity> destino : destinos.entrySet()) {
            if (!porLaboratorio.containsKey(destino.getKey())) {
                if (referenciaUsuario == null) {
                    referenciaUsuario = this.usuarioRepository.getReferenceById(idUsuario);
                }
                cambios.add(UsuarioLaboratorioEntity.builder()
                        .id(new UsuarioLaboratorioId(idUsuario, destino.getKey()))
                        .usuario(referenciaUsuario)
                        .laboratorio(destino.getValue())
                        .activo(true)
                        .build());
            }
        }
        if (!cambios.isEmpty()) {
            this.asignacionRepository.saveAllAndFlush(cambios);
        }
        return this.obtenerAsignacionesActivas(usuario);
    }

    private Usuario buscarUsuarioPublico(Integer idUsuario) {
        return this.usuarioRepository.findPublicByIdUsuario(idUsuario)
                .orElseThrow(() -> this.usuarioInexistente(idUsuario));
    }

    private ResourceNotFoundException usuarioInexistente(Integer idUsuario) {
        return new ResourceNotFoundException("No existe un usuario con el ID " + idUsuario + ".");
    }

    private LaboratorioEntity buscarLaboratorioActivoParaAsignar(Integer idLaboratorio) {
        // El DELETE de Laboratorio usa el mismo bloqueo y revisa las asignaciones.
        return this.laboratorioRepository.findForUpdateByIdLaboratorioAndActivoTrue(idLaboratorio)
                .orElseThrow(() -> {
                    if (this.laboratorioRepository.existsById(idLaboratorio)) {
                        return new ConflictException("El laboratorio seleccionado está inactivo.");
                    }
                    return new ResourceNotFoundException(
                            "No existe un laboratorio con el ID " + idLaboratorio + ".");
                });
    }

    private UsuarioLaboratorios obtenerAsignacionesActivas(Usuario usuario) {
        List<UsuarioLaboratorio> asignaciones = this.asignacionRepository
                .findAllByUsuario_IdUsuarioAndActivoTrueOrderByLaboratorio_IdLaboratorioAsc(usuario.getId())
                .stream()
                .map(entity -> this.mapper.convert(entity, usuario))
                .toList();
        return UsuarioLaboratorios.builder()
                .usuario(usuario)
                .laboratorios(asignaciones.stream().map(UsuarioLaboratorio::getLaboratorio).toList())
                .build();
    }
}

