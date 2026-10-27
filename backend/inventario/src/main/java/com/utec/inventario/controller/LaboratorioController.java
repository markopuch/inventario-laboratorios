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

import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.dto.request.CambiarEstadoCatalogoRequest;
import com.utec.inventario.dto.request.CreateLaboratorioRequest;
import com.utec.inventario.dto.request.UpdateLaboratorioRequest;
import com.utec.inventario.dto.response.LaboratorioResponse;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.service.LaboratorioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Laboratorios", description = "Ubicaciones del inventario; actividad y estado operativo son independientes.")
@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;
    private final LaboratorioMapper mapper;

    @Autowired
    public LaboratorioController(LaboratorioService laboratorioService, LaboratorioMapper mapper) {
        this.laboratorioService = laboratorioService;
        this.mapper = mapper;
    }

    @Operation(summary = "Listar laboratorios activos",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios. Puede incluir laboratorios no asignados al usuario; para alcance propio usar /api/auth/me/laboratorios.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<LaboratorioResponse> listarLaboratorios() {
        return this.mapper.toResponse(this.laboratorioService.listarLaboratorios());
    }

    @Operation(summary = "Consultar un laboratorio activo",
            description = "ADMIN, GESTOR y LECTOR. Consulta global del catálogo activo, independiente de las asignaciones de laboratorios.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LaboratorioResponse obtenerLaboratorio(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.laboratorioService.obtenerLaboratorio(id));
    }

    @Operation(summary = "Crear un laboratorio",
            description = "Solo ADMIN. Requiere área activa y código único global sin distinguir mayúsculas, incluidos inactivos. Estado operativo predeterminado OPERATIVO, independiente de activo.")
    @ApiResponse(responseCode = "201", description = "Laboratorio creado",
            content = @Content(schema = @Schema(implementation = LaboratorioResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PostMapping
    public ResponseEntity<LaboratorioResponse> crearLaboratorio(
            @Valid @RequestBody CreateLaboratorioRequest request) {
        Laboratorio laboratorio = this.mapper.convert(request);
        Laboratorio creado = this.laboratorioService.crearLaboratorio(laboratorio);
        LaboratorioResponse response = this.mapper.toResponse(creado);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Actualizar un laboratorio activo",
            description = "Solo ADMIN. Permite cambiar datos, área y estado operativo OPERATIVO/MANTENIMIENTO. Omitir estadoOperativo conserva el anterior; no altera su actividad lógica.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LaboratorioResponse actualizarLaboratorio(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateLaboratorioRequest request) {
        Laboratorio cambios = this.mapper.convert(request);
        Laboratorio actualizado = this.laboratorioService.actualizarLaboratorio(id, cambios);
        return this.mapper.toResponse(actualizado);
    }

    @Operation(summary = "Activar o desactivar un laboratorio",
            description = "Solo ADMIN. Activar requiere área activa. Desactivar se bloquea por equipos no BAJA o asignaciones activas, incluso de usuarios inactivos; movimientos históricos por sí solos no bloquean.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.OK)
    public LaboratorioResponse cambiarEstadoLaboratorio(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody CambiarEstadoCatalogoRequest request) {
        return this.mapper.toResponse(this.laboratorioService.cambiarEstadoLaboratorio(id, request.getActivo()));
    }

    @Operation(summary = "Dar de baja un laboratorio",
            description = "Solo ADMIN. Baja lógica; rechaza equipos no BAJA o asignaciones activas. Conserva historia y no confunde baja con estado operativo MANTENIMIENTO.")
    @ApiResponse(responseCode = "204", description = "Laboratorio dado de baja; sin cuerpo", content = @Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarLaboratorio(@Positive @PathVariable("id") Integer id) {
        this.laboratorioService.eliminarLaboratorio(id);
    }
}
