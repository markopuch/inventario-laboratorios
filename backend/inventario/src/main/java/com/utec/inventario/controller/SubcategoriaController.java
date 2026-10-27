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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.dto.request.CambiarEstadoCatalogoRequest;
import com.utec.inventario.dto.request.CreateSubcategoriaRequest;
import com.utec.inventario.dto.request.UpdateSubcategoriaRequest;
import com.utec.inventario.dto.response.SubcategoriaResponse;
import com.utec.inventario.mapper.SubcategoriaMapper;
import com.utec.inventario.service.SubcategoriaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Subcategorías", description = "Clasificación dependiente de una categoría; lectura global y escritura ADMIN.")
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

    @Operation(summary = "Listar subcategorías activas",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SubcategoriaResponse> listarSubcategorias() {
        return this.mapper.toResponse(this.subcategoriaService.listarSubcategorias());
    }

    @Operation(summary = "Consultar una subcategoría activa",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SubcategoriaResponse obtenerSubcategoria(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.subcategoriaService.obtenerSubcategoria(id));
    }

    @Operation(summary = "Crear una subcategoría",
            description = "Solo ADMIN. Requiere categoría activa y nombre único dentro de ella sin distinguir mayúsculas, incluidos registros inactivos.")
    @ApiResponse(responseCode = "201", description = "Subcategoría creada",
            content = @Content(schema = @Schema(implementation = SubcategoriaResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
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

    @Operation(summary = "Actualizar una subcategoría activa",
            description = "Solo ADMIN. Reemplaza los datos editables y permite cambiar a otra categoría activa; respeta la unicidad por categoría.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SubcategoriaResponse actualizarSubcategoria(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateSubcategoriaRequest request) {
        Subcategoria cambios = this.mapper.convert(request);
        Subcategoria subcategoriaActualizada = this.subcategoriaService.actualizarSubcategoria(id, cambios);
        return this.mapper.toResponse(subcategoriaActualizada);
    }

    @Operation(summary = "Activar o desactivar una subcategoría",
            description = "Solo ADMIN. Activar requiere padre activo; desactivar exige que no tenga equipos distintos de BAJA. Conserva el registro histórico.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.OK)
    public SubcategoriaResponse cambiarEstadoSubcategoria(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody CambiarEstadoCatalogoRequest request) {
        return this.mapper.toResponse(this.subcategoriaService.cambiarEstadoSubcategoria(id, request.getActivo()));
    }

    @Operation(summary = "Dar de baja una subcategoría",
            description = "Solo ADMIN. Baja lógica; rechaza la operación si existen equipos que no estén BAJA.")
    @ApiResponse(responseCode = "204", description = "Subcategoría dada de baja; sin cuerpo", content = @Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSubcategoria(@Positive @PathVariable("id") Integer id) {
        this.subcategoriaService.eliminarSubcategoria(id);
    }
}
