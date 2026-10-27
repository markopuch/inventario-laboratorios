package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.utec.inventario.dto.request.FiltroReporteRequest;
import com.utec.inventario.dto.response.*;
import com.utec.inventario.mapper.ReporteMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.ReporteService;
import jakarta.validation.Valid;

@Tag(name = "Reportes", description = "Agregados calculados por servidor según filtros y alcance; no son inventarios detallados.")
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final ReporteService service;
    private final ReporteMapper mapper;
    @Autowired
    public ReporteController(ReporteService service, ReporteMapper mapper) {
        this.service = service; this.mapper = mapper;
    }
    @Operation(summary = "Consultar el resumen agregado del inventario",
            description = "ADMIN global; GESTOR y LECTOR dentro de su alcance. Incluye equipos, movimientos y mantenimientos. Los filtros se aplican al conjunto correspondiente y no conceden acceso adicional.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/resumen")
    public ResumenReporteResponse resumen(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails actor,
            @ParameterObject @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponse(service.resumen(actor.getId(), mapper.convert(filtro)));
    }
    @Operation(summary = "Contar equipos por estado",
            description = "ADMIN global; GESTOR y LECTOR según laboratorio actual autorizado. FechaDesde y fechaHasta filtran la creación por días UTC; devuelve grupos con sus totales.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/equipos/por-estado")
    public List<ConteoReporteResponse> equiposPorEstado(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails actor,
            @ParameterObject @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponseConteos(service.equiposPorEstado(actor.getId(), mapper.convert(filtro)));
    }
    @Operation(summary = "Contar equipos por laboratorio",
            description = "ADMIN global; GESTOR y LECTOR según ubicación actual autorizada. Aplica filtros de equipo y jerarquía; devuelve ID, código, nombre y total de cada laboratorio con resultados.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/equipos/por-laboratorio")
    public List<EquiposLaboratorioReporteResponse> equiposPorLaboratorio(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails actor,
            @ParameterObject @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponseLaboratorios(service.equiposPorLaboratorio(actor.getId(), mapper.convert(filtro)));
    }
    @Operation(summary = "Resumir movimientos por tipo",
            description = "ADMIN global; GESTOR y LECTOR según origen o destino autorizado. El rango se aplica a la fecha del movimiento por días UTC y conserva la visibilidad histórica.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/movimientos")
    public MovimientosReporteResponse movimientos(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails actor,
            @ParameterObject @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponse(service.movimientos(actor.getId(), mapper.convert(filtro)));
    }
    @Operation(summary = "Resumir mantenimientos por estado y tipo",
            description = "ADMIN global; GESTOR y LECTOR según laboratorio actual del equipo. El rango usa fecha programada de calendario; los filtros de estado y tipo de mantenimiento afectan este agregado.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/mantenimientos")
    public MantenimientosReporteResponse mantenimientos(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails actor,
            @ParameterObject @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponse(service.mantenimientos(actor.getId(), mapper.convert(filtro)));
    }
}

