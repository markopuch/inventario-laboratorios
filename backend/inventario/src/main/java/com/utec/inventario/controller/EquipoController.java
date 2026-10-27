package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.dto.request.CreateEquipoRequest;
import com.utec.inventario.dto.request.UpdateEquipoRequest;
import com.utec.inventario.dto.response.EquipoResponse;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.EquipoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Equipos", description = "Inventario, filtros, custodia y baja lógica; alcance por laboratorio actual.")
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;
    private final EquipoMapper mapper;

    @Autowired
    public EquipoController(EquipoService equipoService, EquipoMapper mapper) {
        this.equipoService = equipoService;
        this.mapper = mapper;
    }

    @Operation(summary = "Listar equipos con filtros combinables",
            description = "ADMIN consulta globalmente, incluso BAJA o laboratorios inactivos. GESTOR y LECTOR consultan según su laboratorio actual autorizado. Los cuatro filtros se combinan y no amplían el alcance.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EquipoResponse> listarEquipos(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @RequestParam(value = "estado", required = false) EstadoEquipo estado,
            @Positive @RequestParam(value = "idLaboratorio", required = false) Integer idLaboratorio,
            @Positive @RequestParam(value = "idSubcategoria", required = false) Integer idSubcategoria,
            @RequestParam(value = "requiereMantenimiento", required = false) Boolean requiereMantenimiento) {
        return this.mapper.toResponse(this.equipoService.listarEquipos(
                principal.getId(), estado, idLaboratorio, idSubcategoria, requiereMantenimiento));
    }

    @Operation(summary = "Consultar el detalle de un equipo",
            description = "ADMIN global; GESTOR y LECTOR según laboratorio actual autorizado. BAJA conserva lectura e historial; ser responsable no concede acceso.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EquipoResponse obtenerEquipo(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.equipoService.obtenerEquipo(principal.getId(), id));
    }

    @Operation(summary = "Registrar un equipo",
            description = "ADMIN o GESTOR con alcance en el laboratorio activo. Requiere subcategoría activa; código y series deben ser únicos. BAJA no se admite como estado de alta.")
    @ApiResponse(responseCode = "201", description = "Equipo creado",
            content = @Content(schema = @Schema(implementation = EquipoResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PostMapping
    public ResponseEntity<EquipoResponse> crearEquipo(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Valid @RequestBody CreateEquipoRequest request) {
        EquipoResponse response = this.mapper.toResponse(
                this.equipoService.crearEquipo(principal.getId(), this.mapper.convert(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Actualizar un equipo sin trasladarlo",
            description = "ADMIN o GESTOR con alcance actual. No admite código, laboratorio ni metadatos inmutables; el traslado tiene su propia ruta. BAJA y mantenimiento EN_PROCESO impiden editar.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EquipoResponse actualizarEquipo(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id, @Valid @RequestBody UpdateEquipoRequest request) {
        return this.mapper.toResponse(
                this.equipoService.actualizarEquipo(principal.getId(), id, this.mapper.convert(request)));
    }

    @Operation(summary = "Dar de baja un equipo",
            description = "ADMIN o GESTOR con alcance actual. Establece BAJA sin borrar el equipo ni su historia. Rechaza un equipo ya BAJA o con mantenimiento EN_PROCESO.")
    @ApiResponse(responseCode = "204", description = "Equipo dado de baja; sin cuerpo", content = @Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEquipo(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("id") Integer id) {
        this.equipoService.eliminarEquipo(principal.getId(), id);
    }
}

