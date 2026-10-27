package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.dto.request.CambiarEstadoCatalogoRequest;
import com.utec.inventario.dto.request.CreateCategoriaRequest;
import com.utec.inventario.dto.request.UpdateCategoriaRequest;
import com.utec.inventario.dto.response.CategoriaResponse;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.service.CategoriaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Categorías", description = "Clasificación principal de equipos; lectura global y escritura exclusiva de ADMIN.")
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

    @Operation(summary = "Listar categorías activas",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoriaResponse> listarCategorias() {
        return this.mapper.toResponse(this.categoriaService.listarCategorias());
    }

    @Operation(summary = "Consultar una categoría activa",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios. Un ID entero inexistente, incluido cero o negativo, conserva la respuesta histórica de recurso no encontrado.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoriaResponse obtenerCategoria(@PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.categoriaService.obtenerCategoria(id));
    }

    @Operation(summary = "Crear una categoría",
            description = "Solo ADMIN. Nombre único global sin distinguir mayúsculas, incluidos registros inactivos; recorta los textos de catálogo.")
    @ApiResponse(responseCode = "201", description = "Categoría creada",
            content = @Content(schema = @Schema(implementation = CategoriaResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
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

    @Operation(summary = "Actualizar una categoría activa",
            description = "Solo ADMIN. Reemplaza nombre y descripción; el nombre permanece reservado incluso después de la baja. Un opcional omitido o nulo se limpia.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoriaResponse actualizarCategoria(
            @PathVariable("id") Integer id, @Valid @RequestBody UpdateCategoriaRequest request) {
        Categoria cambios = this.mapper.convert(request);
        Categoria categoriaActualizada = this.categoriaService.actualizarCategoria(id, cambios);
        return this.mapper.toResponse(categoriaActualizada);
    }

    @Operation(summary = "Activar o desactivar una categoría",
            description = "Solo ADMIN. La desactivación exige que no tenga subcategorías activas; reactivar conserva el registro y las reservas de nombre.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.OK)
    public CategoriaResponse cambiarEstadoCategoria(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody CambiarEstadoCatalogoRequest request) {
        return this.mapper.toResponse(this.categoriaService.cambiarEstadoCategoria(id, request.getActivo()));
    }

    @Operation(summary = "Dar de baja una categoría",
            description = "Solo ADMIN. Baja lógica sin borrar el registro; rechaza categorías con subcategorías activas.")
    @ApiResponse(responseCode = "204", description = "Categoría dada de baja; sin cuerpo", content = @Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCategoria(@PathVariable("id") Integer id) {
        this.categoriaService.eliminarCategoria(id);
    }
}
