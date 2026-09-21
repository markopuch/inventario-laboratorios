package com.utec.inventario.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.dto.response.SubcategoriaResponse;
import com.utec.inventario.mapper.SubcategoriaMapper;
import com.utec.inventario.service.SubcategoriaService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/categorias/{idCategoria}/subcategorias")
public class CategoriaSubcategoriaController {

    private final SubcategoriaService subcategoriaService;
    private final SubcategoriaMapper mapper;

    @Autowired
    public CategoriaSubcategoriaController(SubcategoriaService subcategoriaService, SubcategoriaMapper mapper) {
        this.subcategoriaService = subcategoriaService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SubcategoriaResponse> listarPorCategoria(
            @Positive @PathVariable("idCategoria") Integer idCategoria) {
        return this.mapper.toResponse(this.subcategoriaService.listarPorCategoria(idCategoria));
    }
}
