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

import com.utec.inventario.dto.response.MovimientoEquipoResponse;
import com.utec.inventario.mapper.MovimientoEquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.MovimientoEquipoService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoEquipoController {

    private final MovimientoEquipoService movimientoService;
    private final MovimientoEquipoMapper mapper;

    @Autowired
    public MovimientoEquipoController(MovimientoEquipoService movimientoService, MovimientoEquipoMapper mapper) {
        this.movimientoService = movimientoService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MovimientoEquipoResponse> listarMovimientos(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @RequestParam(value = "idLaboratorio", required = false) Integer idLaboratorio) {
        return this.mapper.toResponse(this.movimientoService.listarMovimientos(principal.getId(), idLaboratorio));
    }
}

