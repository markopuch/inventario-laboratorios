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

import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.dto.request.CreateSubcategoriaRequest;
import com.utec.inventario.dto.request.UpdateSubcategoriaRequest;
import com.utec.inventario.dto.response.SubcategoriaResponse;
import com.utec.inventario.mapper.SubcategoriaMapper;
import com.utec.inventario.service.SubcategoriaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/subcategorias")
public class SubcategoriaController {

    private final SubcategoriaService subcategoriaService;
    private final SubcategoriaMapper mapper;

    @Autowired
    public SubcategoriaController(SubcategoriaService subcategoriaService, SubcategoriaMapper mapper) {
        this.subcategoriaService = subcategoriaService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SubcategoriaResponse> listarSubcategorias() {
        return this.mapper.toResponse(this.subcategoriaService.listarSubcategorias());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SubcategoriaResponse obtenerSubcategoria(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.subcategoriaService.obtenerSubcategoria(id));
    }

    @PostMapping
    public ResponseEntity<SubcategoriaResponse> crearSubcategoria(
            @Valid @RequestBody CreateSubcategoriaRequest request) {
        Subcategoria subcategoria = this.mapper.convert(request);
        Subcategoria subcategoriaCreada = this.subcategoriaService.crearSubcategoria(subcategoria);
        SubcategoriaResponse response = this.mapper.toResponse(subcategoriaCreada);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SubcategoriaResponse actualizarSubcategoria(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateSubcategoriaRequest request) {
        Subcategoria cambios = this.mapper.convert(request);
        Subcategoria subcategoriaActualizada = this.subcategoriaService.actualizarSubcategoria(id, cambios);
        return this.mapper.toResponse(subcategoriaActualizada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSubcategoria(@Positive @PathVariable("id") Integer id) {
        this.subcategoriaService.eliminarSubcategoria(id);
    }
}
