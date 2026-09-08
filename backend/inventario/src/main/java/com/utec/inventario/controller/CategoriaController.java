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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.dto.request.CreateCategoriaRequest;
import com.utec.inventario.dto.request.UpdateCategoriaRequest;
import com.utec.inventario.dto.response.CategoriaResponse;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.service.CategoriaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final CategoriaMapper mapper;

    @Autowired
    public CategoriaController(CategoriaService categoriaService, CategoriaMapper mapper) {
        this.categoriaService = categoriaService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoriaResponse> listarCategorias() {
        return this.mapper.toResponse(this.categoriaService.listarCategorias());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoriaResponse obtenerCategoria(@PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.categoriaService.obtenerCategoria(id));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crearCategoria(
            @Valid @RequestBody CreateCategoriaRequest request) {
        Categoria categoria = this.mapper.convert(request);
        Categoria categoriaCreada = this.categoriaService.crearCategoria(categoria);
        CategoriaResponse response = this.mapper.toResponse(categoriaCreada);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoriaResponse actualizarCategoria(
            @PathVariable("id") Integer id, @Valid @RequestBody UpdateCategoriaRequest request) {
        Categoria cambios = this.mapper.convert(request);
        Categoria categoriaActualizada = this.categoriaService.actualizarCategoria(id, cambios);
        return this.mapper.toResponse(categoriaActualizada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCategoria(@PathVariable("id") Integer id) {
        this.categoriaService.eliminarCategoria(id);
    }
}
