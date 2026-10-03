package com.utec.inventario.controller;

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

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final ReporteService service;
    private final ReporteMapper mapper;
    @Autowired
    public ReporteController(ReporteService service, ReporteMapper mapper) {
        this.service = service; this.mapper = mapper;
    }
    @GetMapping("/resumen")
    public ResumenReporteResponse resumen(@AuthenticationPrincipal UserInfoDetails actor,
            @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponse(service.resumen(actor.getId(), mapper.convert(filtro)));
    }
    @GetMapping("/equipos/por-estado")
    public List<ConteoReporteResponse> equiposPorEstado(@AuthenticationPrincipal UserInfoDetails actor,
            @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponseConteos(service.equiposPorEstado(actor.getId(), mapper.convert(filtro)));
    }
    @GetMapping("/equipos/por-laboratorio")
    public List<EquiposLaboratorioReporteResponse> equiposPorLaboratorio(@AuthenticationPrincipal UserInfoDetails actor,
            @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponseLaboratorios(service.equiposPorLaboratorio(actor.getId(), mapper.convert(filtro)));
    }
    @GetMapping("/movimientos")
    public MovimientosReporteResponse movimientos(@AuthenticationPrincipal UserInfoDetails actor,
            @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponse(service.movimientos(actor.getId(), mapper.convert(filtro)));
    }
    @GetMapping("/mantenimientos")
    public MantenimientosReporteResponse mantenimientos(@AuthenticationPrincipal UserInfoDetails actor,
            @Valid @ModelAttribute FiltroReporteRequest filtro) {
        return mapper.toResponse(service.mantenimientos(actor.getId(), mapper.convert(filtro)));
    }
}

