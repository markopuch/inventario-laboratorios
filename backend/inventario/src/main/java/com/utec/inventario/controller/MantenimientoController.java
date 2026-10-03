package com.utec.inventario.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.dto.request.CambiarEstadoMantenimientoRequest;
import com.utec.inventario.dto.request.CreateMantenimientoRequest;
import com.utec.inventario.dto.request.FiltroMantenimientoRequest;
import com.utec.inventario.dto.request.UpdateMantenimientoRequest;
import com.utec.inventario.dto.response.MantenimientoResponse;
import com.utec.inventario.mapper.MantenimientoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.MantenimientoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {
    private final MantenimientoService mantenimientoService;
    private final MantenimientoMapper mapper;

    @Autowired
    public MantenimientoController(MantenimientoService mantenimientoService, MantenimientoMapper mapper) {
        this.mantenimientoService = mantenimientoService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MantenimientoResponse> listar(@AuthenticationPrincipal UserInfoDetails principal,
            @Valid @ModelAttribute FiltroMantenimientoRequest filtros) {
        return this.mapper.toResponse(this.mantenimientoService.listarMantenimientos(principal.getId(),
                filtros.getIdEquipo(), filtros.getIdLaboratorio(), filtros.getEstado(), filtros.getTipo(),
                filtros.getFechaDesde(), filtros.getFechaHasta()));
    }

    @GetMapping("/{id}")
    public MantenimientoResponse obtener(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.mantenimientoService.obtenerMantenimiento(principal.getId(), id));
    }

    @PostMapping
    public ResponseEntity<MantenimientoResponse> crear(@AuthenticationPrincipal UserInfoDetails principal,
            @Valid @RequestBody CreateMantenimientoRequest request) {
        MantenimientoResponse response = this.mapper.toResponse(this.mantenimientoService.crearMantenimiento(
                principal.getId(), this.mapper.convert(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public MantenimientoResponse actualizar(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id, @Valid @RequestBody UpdateMantenimientoRequest request) {
        return this.mapper.toResponse(this.mantenimientoService.actualizarMantenimiento(principal.getId(), id, this.mapper.convert(request)));
    }

    @PatchMapping("/{id}/estado")
    public MantenimientoResponse cambiarEstado(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id, @Valid @RequestBody CambiarEstadoMantenimientoRequest request) {
        return this.mapper.toResponse(this.mantenimientoService.cambiarEstado(principal.getId(), id, request.getEstado(), request.getObservaciones()));
    }
}
