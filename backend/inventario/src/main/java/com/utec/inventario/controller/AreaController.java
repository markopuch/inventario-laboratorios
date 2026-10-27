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

import com.utec.inventario.domain.Area;
import com.utec.inventario.dto.request.CambiarEstadoCatalogoRequest;
import com.utec.inventario.dto.request.CreateAreaRequest;
import com.utec.inventario.dto.request.UpdateAreaRequest;
import com.utec.inventario.dto.response.AreaResponse;
import com.utec.inventario.mapper.AreaMapper;
import com.utec.inventario.service.AreaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Áreas", description = "Segundo nivel de organización, dependiente de una sede.")
@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;
    private final AreaMapper mapper;

    @Autowired
    public AreaController(AreaService areaService, AreaMapper mapper) {
        this.areaService = areaService;
        this.mapper = mapper;
    }

    @Operation(summary = "Listar áreas activas",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AreaResponse> listarAreas() {
        return this.mapper.toResponse(this.areaService.listarAreas());
    }

    @Operation(summary = "Consultar un área activa",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AreaResponse obtenerArea(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.areaService.obtenerArea(id));
    }

    @Operation(summary = "Crear un área",
            description = "Solo ADMIN. Requiere sede activa; nombre único dentro de la sede sin distinguir mayúsculas, incluidos registros inactivos.")
    @ApiResponse(responseCode = "201", description = "Área creada",
            content = @Content(schema = @Schema(implementation = AreaResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PostMapping
    public ResponseEntity<AreaResponse> crearArea(
            @Valid @RequestBody CreateAreaRequest request) {
        Area area = this.mapper.convert(request);
        Area creado = this.areaService.crearArea(area);
        AreaResponse response = this.mapper.toResponse(creado);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Actualizar un área activa",
            description = "Solo ADMIN. Reemplaza datos editables y permite cambiar a una sede activa; conserva la unicidad por sede.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AreaResponse actualizarArea(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateAreaRequest request) {
        Area cambios = this.mapper.convert(request);
        Area actualizado = this.areaService.actualizarArea(id, cambios);
        return this.mapper.toResponse(actualizado);
    }

    @Operation(summary = "Activar o desactivar un área",
            description = "Solo ADMIN. Activar requiere sede activa; desactivar exige no tener laboratorios activos. No borra historia.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.OK)
    public AreaResponse cambiarEstadoArea(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody CambiarEstadoCatalogoRequest request) {
        return this.mapper.toResponse(this.areaService.cambiarEstadoArea(id, request.getActivo()));
    }

    @Operation(summary = "Dar de baja un área",
            description = "Solo ADMIN. Baja lógica; rechaza la operación si conserva laboratorios activos.")
    @ApiResponse(responseCode = "204", description = "Área dada de baja; sin cuerpo", content = @Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarArea(@Positive @PathVariable("id") Integer id) {
        this.areaService.eliminarArea(id);
    }
}
