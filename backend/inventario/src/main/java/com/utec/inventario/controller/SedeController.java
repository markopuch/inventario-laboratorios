package com.utec.inventario.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.domain.Sede;
import com.utec.inventario.dto.request.CreateSedeRequest;
import com.utec.inventario.dto.request.UpdateSedeRequest;
import com.utec.inventario.dto.response.SedeResponse;
import com.utec.inventario.mapper.SedeMapper;
import com.utec.inventario.service.SedeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/sedes")
public class SedeController {

    private final SedeService sedeService;
    private final SedeMapper mapper;

    @Autowired
    public SedeController(SedeService sedeService, SedeMapper mapper) {
        this.sedeService = sedeService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SedeResponse> listarSedes() {
        return this.mapper.toResponse(this.sedeService.listarSedes());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SedeResponse obtenerSede(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.sedeService.obtenerSede(id));
    }

    @PostMapping
    public ResponseEntity<SedeResponse> crearSede(
            @Valid @RequestBody CreateSedeRequest request) {
        Sede sede = this.mapper.convert(request);
        Sede creado = this.sedeService.crearSede(sede);
        SedeResponse response = this.mapper.toResponse(creado);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SedeResponse actualizarSede(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateSedeRequest request) {
        Sede cambios = this.mapper.convert(request);
        Sede actualizado = this.sedeService.actualizarSede(id, cambios);
        return this.mapper.toResponse(actualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSede(@Positive @PathVariable("id") Integer id) {
        this.sedeService.eliminarSede(id);
    }
}
