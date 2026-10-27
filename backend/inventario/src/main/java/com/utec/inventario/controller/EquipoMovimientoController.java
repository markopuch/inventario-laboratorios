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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.domain.TrasladoEquipo;
import com.utec.inventario.dto.request.TrasladarEquipoRequest;
import com.utec.inventario.dto.response.MovimientoEquipoResponse;
import com.utec.inventario.dto.response.TrasladoEquipoResponse;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.mapper.MovimientoEquipoMapper;
import com.utec.inventario.security.UserInfoDetails;
import com.utec.inventario.service.MovimientoEquipoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Movimientos", description = "Traslados transaccionales e historial inmutable con alcance por origen o destino.")
@RestController
@RequestMapping("/api/equipos/{idEquipo}")
public class EquipoMovimientoController {

    private final MovimientoEquipoService movimientoService;
    private final MovimientoEquipoMapper mapper;
    private final EquipoMapper equipoMapper;

    @Autowired
    public EquipoMovimientoController(MovimientoEquipoService movimientoService,
            MovimientoEquipoMapper mapper, EquipoMapper equipoMapper) {
        this.movimientoService = movimientoService;
        this.mapper = mapper;
        this.equipoMapper = equipoMapper;
    }

    @Operation(summary = "Trasladar un equipo y registrar su movimiento",
            description = "ADMIN o GESTOR autorizado en origen y destino. Actualiza ubicación e historial en una transacción; servidor fija origen real, actor, tipo y fecha. Rechaza BAJA, mantenimiento EN_PROCESO o destino igual/inactivo.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PostMapping("/traslados")
    @ResponseStatus(HttpStatus.OK)
    public TrasladoEquipoResponse trasladarEquipo(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("idEquipo") Integer idEquipo,
            @Valid @RequestBody TrasladarEquipoRequest request) {
        TrasladoEquipo traslado = this.movimientoService.trasladarEquipo(principal.getId(), idEquipo,
                request.getIdLaboratorioDestino(), request.getMotivo(), request.getUbicacionInternaDestino());
        return TrasladoEquipoResponse.builder()
                .equipo(this.equipoMapper.toResponse(traslado.getEquipo()))
                .movimiento(this.mapper.toResponse(traslado.getMovimiento()))
                .build();
    }

    @Operation(summary = "Consultar el historial visible de un equipo",
            description = "ADMIN global. GESTOR y LECTOR ven movimientos por origen o destino autorizado, aunque el equipo actual quede fuera de alcance. El historial se conserva tras BAJA y se ordena del más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/movimientos")
    @ResponseStatus(HttpStatus.OK)
    public List<MovimientoEquipoResponse> listarPorEquipo(@Parameter(hidden = true) @AuthenticationPrincipal UserInfoDetails principal,
            @Positive @PathVariable("idEquipo") Integer idEquipo) {
        return this.mapper.toResponse(this.movimientoService.listarPorEquipo(principal.getId(), idEquipo));
    }
}

