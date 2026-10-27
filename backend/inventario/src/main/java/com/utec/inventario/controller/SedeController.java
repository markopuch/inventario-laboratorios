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

import com.utec.inventario.domain.Sede;
import com.utec.inventario.dto.request.CambiarEstadoCatalogoRequest;
import com.utec.inventario.dto.request.CreateSedeRequest;
import com.utec.inventario.dto.request.UpdateSedeRequest;
import com.utec.inventario.dto.response.SedeResponse;
import com.utec.inventario.mapper.SedeMapper;
import com.utec.inventario.service.SedeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Sedes", description = "Primer nivel de organización; lectura global y escritura exclusiva de ADMIN.")
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

    @Operation(summary = "Listar sedes activas",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SedeResponse> listarSedes() {
        return this.mapper.toResponse(this.sedeService.listarSedes());
    }

    @Operation(summary = "Consultar una sede activa",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SedeResponse obtenerSede(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.sedeService.obtenerSede(id));
    }

    @Operation(summary = "Crear una sede",
            description = "Solo ADMIN. Crea una sede activa con nombre obligatorio; este catálogo no exige unicidad de nombre.")
    @ApiResponse(responseCode = "201", description = "Sede creada",
            content = @Content(schema = @Schema(implementation = SedeResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
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

    @Operation(summary = "Actualizar una sede activa",
            description = "Solo ADMIN. Reemplaza nombre, dirección, distrito y departamento de una sede activa; los opcionales omitidos o nulos se limpian.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SedeResponse actualizarSede(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateSedeRequest request) {
        Sede cambios = this.mapper.convert(request);
        Sede actualizado = this.sedeService.actualizarSede(id, cambios);
        return this.mapper.toResponse(actualizado);
    }

    @Operation(summary = "Activar o desactivar una sede",
            description = "Solo ADMIN. Desactivar exige que no existan áreas activas; reactivar conserva identidad e historia.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.OK)
    public SedeResponse cambiarEstadoSede(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody CambiarEstadoCatalogoRequest request) {
        return this.mapper.toResponse(this.sedeService.cambiarEstadoSede(id, request.getActivo()));
    }

    @Operation(summary = "Dar de baja una sede",
            description = "Solo ADMIN. Baja lógica sin borrar la sede; rechaza la operación si tiene áreas activas.")
    @ApiResponse(responseCode = "204", description = "Sede dada de baja; sin cuerpo", content = @Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSede(@Positive @PathVariable("id") Integer id) {
        this.sedeService.eliminarSede(id);
    }
}
