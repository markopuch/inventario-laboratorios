package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.dto.request.CambiarEstadoMantenimientoRequest;
import com.utec.inventario.dto.request.CreateMantenimientoRequest;
import com.utec.inventario.dto.request.FiltroMantenimientoRequest;
import com.utec.inventario.dto.request.UpdateMantenimientoRequest;
import com.utec.inventario.dto.response.MantenimientoResponse;
import com.utec.inventario.mapper.MantenimientoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.MantenimientoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Mantenimientos", description = "Programación y ciclo de mantenimiento de equipos, con alcance por ubicación actual.")
@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {
    private final MantenimientoService mantenimientoService;
    private final MantenimientoMapper mapper;

    @Autowired
    public MantenimientoController(MantenimientoService mantenimientoService, MantenimientoMapper mapper) {
        this.mantenimientoService = mantenimientoService;
        this.mapper = mapper;
    }

    @Operation(summary = "Listar mantenimientos con filtros",
            description = "ADMIN global; GESTOR y LECTOR según laboratorio actual del equipo. Combina equipo, laboratorio, estado, tipo y rango de fecha programada. Un laboratorio explícito fuera de alcance se rechaza.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping
    public List<MantenimientoResponse> listar(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @ParameterObject @Valid @ModelAttribute FiltroMantenimientoRequest filtros) {
        return this.mapper.toResponse(this.mantenimientoService.listarMantenimientos(principal.getId(),
                filtros.getIdEquipo(), filtros.getIdLaboratorio(), filtros.getEstado(), filtros.getTipo(),
                filtros.getFechaDesde(), filtros.getFechaHasta()));
    }

    @Operation(summary = "Consultar un mantenimiento",
            description = "ADMIN global; GESTOR y LECTOR según laboratorio actual del equipo, no según su ubicación histórica.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    public MantenimientoResponse obtener(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.mantenimientoService.obtenerMantenimiento(principal.getId(), id));
    }

    @Operation(summary = "Programar un mantenimiento",
            description = "ADMIN o GESTOR con alcance en el laboratorio actual. Crea PROGRAMADO para un equipo vigente de laboratorio activo; servidor fija ID y fechas de control.")
    @ApiResponse(responseCode = "201", description = "Mantenimiento programado",
            content = @Content(schema = @Schema(implementation = MantenimientoResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PostMapping
    public ResponseEntity<MantenimientoResponse> crear(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Valid @RequestBody CreateMantenimientoRequest request) {
        MantenimientoResponse response = this.mapper.toResponse(this.mantenimientoService.crearMantenimiento(
                principal.getId(), this.mapper.convert(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Editar un mantenimiento programado",
            description = "ADMIN o GESTOR con alcance actual. Solo se edita PROGRAMADO y con equipo no BAJA. Reemplaza datos editables sin cambiar equipo ni estado.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping("/{id}")
    public MantenimientoResponse actualizar(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id, @Valid @RequestBody UpdateMantenimientoRequest request) {
        return this.mapper.toResponse(this.mantenimientoService.actualizarMantenimiento(principal.getId(), id, this.mapper.convert(request)));
    }

    @Operation(summary = "Cambiar el estado de un mantenimiento",
            description = "ADMIN o GESTOR con alcance actual. PROGRAMADO pasa a EN_PROCESO o CANCELADO; EN_PROCESO a COMPLETADO o CANCELADO. Iniciar guarda el estado previo y bloquea editar, bajar o trasladar equipo; cerrar restaura ese estado.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/estado")
    public MantenimientoResponse cambiarEstado(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id, @Valid @RequestBody CambiarEstadoMantenimientoRequest request) {
        return this.mapper.toResponse(this.mantenimientoService.cambiarEstado(principal.getId(), id, request.getEstado(), request.getObservaciones()));
    }
}
