package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.utec.inventario.dto.request.FiltroAuditoriaRequest;
import com.utec.inventario.dto.request.PaginacionRequest;
import com.utec.inventario.dto.response.AuditoriaResponse;
import com.utec.inventario.dto.response.PaginaAuditoriaResponse;
import com.utec.inventario.mapper.AuditoriaMapper;
import com.utec.inventario.service.AuditoriaService;
import jakarta.validation.Valid;

@Tag(name = "Auditoría", description = "Consulta administrativa de eventos registrados automáticamente por operaciones de negocio.")
@RestController
@RequestMapping("/api/admin/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;
    private final AuditoriaMapper mapper;
    @Autowired
    public AuditoriaController(AuditoriaService service, AuditoriaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }
    @Operation(summary = "Consultar eventos de auditoría",
            description = "Solo ADMIN. Seis filtros por entidad, registro, actor, acción y días UTC aplicados antes del conteo. page o size activa página base cero (0/20, máximo 100); ambos omitidos conservan el arreglo. Orden fecha DESC e ID DESC, sin exponer secretos.")
    @ApiResponse(responseCode = "200", description = "Arreglo compatible o página si se envía page o size",
            content = @Content(schema = @Schema(oneOf = {AuditoriaResponse[].class, PaginaAuditoriaResponse.class})))
    @GetMapping
    public Object listar(@ParameterObject @Valid @ModelAttribute FiltroAuditoriaRequest filtro,
            @ParameterObject @Valid @ModelAttribute PaginacionRequest paginacion) {
        if (paginacion.isSolicitada()) {
            return this.mapper.toResponse(this.service.listarPaginado(this.mapper.convert(filtro),
                    paginacion.paginaEfectiva(), paginacion.tamanoEfectivo()));
        }
        return this.mapper.toResponse(this.service.listar(this.mapper.convert(filtro)));
    }
}
