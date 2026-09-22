package com.utec.inventario.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.dto.response.EquipoResponse;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.EquipoService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/admin/equipos")
public class AdminEquipoController {

    private final EquipoService equipoService;
    private final EquipoMapper mapper;

    @Autowired
    public AdminEquipoController(EquipoService equipoService, EquipoMapper mapper) {
        this.equipoService = equipoService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EquipoResponse> listarEquipos(@AuthenticationPrincipal UserInfoDetails principal,
            @RequestParam(value = "estado", required = false) EstadoEquipo estado,
            @Positive @RequestParam(value = "idLaboratorio", required = false) Integer idLaboratorio,
            @Positive @RequestParam(value = "idSubcategoria", required = false) Integer idSubcategoria,
            @RequestParam(value = "requiereMantenimiento", required = false) Boolean requiereMantenimiento) {
        return this.mapper.toResponse(this.equipoService.listarEquipos(
                principal.getId(), estado, idLaboratorio, idSubcategoria, requiereMantenimiento));
    }
}

