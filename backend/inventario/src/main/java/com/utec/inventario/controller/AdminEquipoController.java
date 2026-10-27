package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.dto.request.PaginacionRequest;
import com.utec.inventario.dto.response.EquipoResponse;
import com.utec.inventario.dto.response.PaginaEquiposResponse;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.EquipoService;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Equipos", description = "Inventario, filtros, custodia y baja lógica; alcance por laboratorio actual.")
@RestController
@RequestMapping("/api/admin/equipos")
public class AdminEquipoController {

    private final EquipoService equipoService;
    private final EquipoMapper mapper;

    @Autowired
    public AdminEquipoController(EquipoService equipoService, EquipoMapper mapper) {
        this.equipoService = equipoService;
        this.mapper = mapper;
    }

    @Operation(summary = "Listar equipos con alcance administrativo",
            description = "Solo ADMIN, incluso BAJA y laboratorios inactivos. Mismos filtros y búsqueda q literal IgnoreCase que /api/equipos. page o size activa página base cero (0/20, máximo 100); ambos omitidos conservan el arreglo. Orden estable por ID ascendente.")
    @ApiResponse(responseCode = "200", description = "Arreglo compatible o página si se envía page o size",
            content = @Content(schema = @Schema(oneOf = {EquipoResponse[].class, PaginaEquiposResponse.class})))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Object listarEquipos(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @RequestParam(value = "estado", required = false) EstadoEquipo estado,
            @Positive @RequestParam(value = "idLaboratorio", required = false) Integer idLaboratorio,
            @Positive @RequestParam(value = "idSubcategoria", required = false) Integer idSubcategoria,
            @RequestParam(value = "requiereMantenimiento", required = false) Boolean requiereMantenimiento,
            @Size(max = 200) @RequestParam(value = "q", required = false) String q,
            @ParameterObject @Valid @ModelAttribute PaginacionRequest paginacion) {
        if (paginacion.isSolicitada()) {
            return this.mapper.toResponse(this.equipoService.listarEquiposPaginados(principal.getId(), estado,
                    idLaboratorio, idSubcategoria, requiereMantenimiento, q,
                    paginacion.paginaEfectiva(), paginacion.tamanoEfectivo()));
        }
        return this.mapper.toResponse(this.equipoService.listarEquipos(
                principal.getId(), estado, idLaboratorio, idSubcategoria, requiereMantenimiento, q));
    }
}

