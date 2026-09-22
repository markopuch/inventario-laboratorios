package com.utec.inventario.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.dto.request.CreateEquipoRequest;
import com.utec.inventario.dto.request.UpdateEquipoRequest;
import com.utec.inventario.dto.response.EquipoResponse;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.EquipoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;
    private final EquipoMapper mapper;

    @Autowired
    public EquipoController(EquipoService equipoService, EquipoMapper mapper) {
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

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EquipoResponse obtenerEquipo(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.equipoService.obtenerEquipo(principal.getId(), id));
    }

    @PostMapping
    public ResponseEntity<EquipoResponse> crearEquipo(@AuthenticationPrincipal UserInfoDetails principal,
            @Valid @RequestBody CreateEquipoRequest request) {
        EquipoResponse response = this.mapper.toResponse(
                this.equipoService.crearEquipo(principal.getId(), this.mapper.convert(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EquipoResponse actualizarEquipo(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id, @Valid @RequestBody UpdateEquipoRequest request) {
        return this.mapper.toResponse(
                this.equipoService.actualizarEquipo(principal.getId(), id, this.mapper.convert(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEquipo(@AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id) {
        this.equipoService.eliminarEquipo(principal.getId(), id);
    }
}

