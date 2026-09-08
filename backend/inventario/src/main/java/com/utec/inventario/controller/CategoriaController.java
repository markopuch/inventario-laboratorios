package com.utec.inventario.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.dto.request.CreateCategoriaRequest;
import com.utec.inventario.dto.request.UpdateCategoriaRequest;
import com.utec.inventario.dto.response.CategoriaResponse;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.service.CategoriaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;
    private final CategoriaMapper mapper;

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(service.listarCategorias().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> obtenerCategoria(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(mapper.toResponse(service.obtenerCategoria(id)));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crearCategoria(
            @Valid @RequestBody CreateCategoriaRequest request) {
        CategoriaResponse response = mapper.toResponse(service.crearCategoria(mapper.toDomain(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizarCategoria(
            @PathVariable("id") Integer id, @Valid @RequestBody UpdateCategoriaRequest request) {
        return ResponseEntity.ok(mapper.toResponse(
                service.actualizarCategoria(id, mapper.toDomain(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable("id") Integer id) {
        service.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
