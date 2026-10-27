package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Subcategorías", description = "Clasificación dependiente de una categoría; lectura global y escritura ADMIN.")
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

    @Operation(summary = "Listar subcategorías activas de una categoría",
            description = "ADMIN, GESTOR y LECTOR, consulta global. Requiere categoría activa y devuelve únicamente sus hijas activas.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SubcategoriaResponse> listarPorCategoria(
            @Positive @PathVariable("idCategoria") Integer idCategoria) {
        return this.mapper.toResponse(this.subcategoriaService.listarPorCategoria(idCategoria));
    }
}
