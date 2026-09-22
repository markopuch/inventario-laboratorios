package com.utec.inventario.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.domain.TrasladoEquipo;
import com.utec.inventario.dto.request.TrasladarEquipoRequest;
import com.utec.inventario.dto.response.MovimientoEquipoResponse;
import com.utec.inventario.dto.response.TrasladoEquipoResponse;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.mapper.MovimientoEquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.MovimientoEquipoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/equipos/{idEquipo}")
public class EquipoMovimientoController {

    private final MovimientoEquipoService movimientoService;
    private final MovimientoEquipoMapper mapper;
    private final EquipoMapper equipoMapper;

    @Autowired
    public EquipoMovimientoController(MovimientoEquipoService movimientoService,
            MovimientoEquipoMapper mapper, EquipoMapper equipoMapper) {
        this.movimientoService = movimientoService;
        this.mapper = mapper;
        this.equipoMapper = equipoMapper;
    }

    @PostMapping("/traslados")
    @ResponseStatus(HttpStatus.OK)
    public TrasladoEquipoResponse trasladarEquipo(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("idEquipo") Integer idEquipo,
            @Valid @RequestBody TrasladarEquipoRequest request) {
        TrasladoEquipo traslado = this.movimientoService.trasladarEquipo(principal.getId(), idEquipo,
                request.getIdLaboratorioDestino(), request.getMotivo(), request.getUbicacionInternaDestino());
        return TrasladoEquipoResponse.builder()
                .equipo(this.equipoMapper.toResponse(traslado.getEquipo()))
                .movimiento(this.mapper.toResponse(traslado.getMovimiento()))
                .build();
    }

    @GetMapping("/movimientos")
    @ResponseStatus(HttpStatus.OK)
    public List<MovimientoEquipoResponse> listarPorEquipo(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("idEquipo") Integer idEquipo) {
        return this.mapper.toResponse(this.movimientoService.listarPorEquipo(principal.getId(), idEquipo));
    }
}

