package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.dto.response.MovimientoEquipoResponse;
import com.utec.inventario.mapper.MovimientoEquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.MovimientoEquipoService;

import jakarta.validation.constraints.Positive;

@Tag(name = "Movimientos", description = "Traslados transaccionales e historial inmutable con alcance por origen o destino.")
@RestController
@RequestMapping("/api/movimientos")
public class MovimientoEquipoController {

    private final MovimientoEquipoService movimientoService;
    private final MovimientoEquipoMapper mapper;

    @Autowired
    public MovimientoEquipoController(MovimientoEquipoService movimientoService, MovimientoEquipoMapper mapper) {
        this.movimientoService = movimientoService;
        this.mapper = mapper;
    }

    @Operation(summary = "Listar movimientos visibles",
            description = "ADMIN global; GESTOR y LECTOR según asignaciones vigentes a origen o destino. El filtro idLaboratorio compara ambos extremos y no amplía permisos. Los eventos no se modifican directamente.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MovimientoEquipoResponse> listarMovimientos(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @RequestParam(value = "idLaboratorio", required = false) Integer idLaboratorio) {
        return this.mapper.toResponse(this.movimientoService.listarMovimientos(principal.getId(), idLaboratorio));
    }
}

