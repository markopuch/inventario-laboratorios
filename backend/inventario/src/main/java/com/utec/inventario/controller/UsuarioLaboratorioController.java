package com.utec.inventario.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.dto.request.ActualizarLaboratoriosUsuarioRequest;
import com.utec.inventario.dto.response.UsuarioLaboratoriosResponse;
import com.utec.inventario.mapper.UsuarioLaboratorioMapper;
import com.utec.inventario.service.UsuarioLaboratorioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/admin/usuarios/{idUsuario}/laboratorios")
public class UsuarioLaboratorioController {

    private final UsuarioLaboratorioService asignacionService;
    private final UsuarioLaboratorioMapper mapper;

    @Autowired
    public UsuarioLaboratorioController(UsuarioLaboratorioService asignacionService,
            UsuarioLaboratorioMapper mapper) {
        this.asignacionService = asignacionService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public UsuarioLaboratoriosResponse listarAsignacionesActivas(
            @Positive @PathVariable("idUsuario") Integer idUsuario) {
        return this.mapper.toResponse(this.asignacionService.listarAsignacionesActivas(idUsuario));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public UsuarioLaboratoriosResponse reemplazarAsignaciones(
            @Positive @PathVariable("idUsuario") Integer idUsuario,
            @Valid @RequestBody ActualizarLaboratoriosUsuarioRequest request) {
        return this.mapper.toResponse(
                this.asignacionService.reemplazarAsignaciones(idUsuario, request.getIdsLaboratorio()));
    }
}

