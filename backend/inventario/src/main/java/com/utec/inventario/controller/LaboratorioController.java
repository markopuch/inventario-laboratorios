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

import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.dto.request.CreateLaboratorioRequest;
import com.utec.inventario.dto.request.UpdateLaboratorioRequest;
import com.utec.inventario.dto.response.LaboratorioResponse;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.service.LaboratorioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;
    private final LaboratorioMapper mapper;

    @Autowired
    public LaboratorioController(LaboratorioService laboratorioService, LaboratorioMapper mapper) {
        this.laboratorioService = laboratorioService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<LaboratorioResponse> listarLaboratorios() {
        return this.mapper.toResponse(this.laboratorioService.listarLaboratorios());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LaboratorioResponse obtenerLaboratorio(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.laboratorioService.obtenerLaboratorio(id));
    }

    @PostMapping
    public ResponseEntity<LaboratorioResponse> crearLaboratorio(
            @Valid @RequestBody CreateLaboratorioRequest request) {
        Laboratorio laboratorio = this.mapper.convert(request);
        Laboratorio creado = this.laboratorioService.crearLaboratorio(laboratorio);
        LaboratorioResponse response = this.mapper.toResponse(creado);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LaboratorioResponse actualizarLaboratorio(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateLaboratorioRequest request) {
        Laboratorio cambios = this.mapper.convert(request);
        Laboratorio actualizado = this.laboratorioService.actualizarLaboratorio(id, cambios);
        return this.mapper.toResponse(actualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarLaboratorio(@Positive @PathVariable("id") Integer id) {
        this.laboratorioService.eliminarLaboratorio(id);
    }
}
