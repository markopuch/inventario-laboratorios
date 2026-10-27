package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.utec.inventario.dto.request.FiltroAuditoriaRequest;
import com.utec.inventario.dto.response.AuditoriaResponse;
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
            description = "Solo ADMIN, alcance global y solo lectura. Filtra por entidad, registro, actor, acción y días UTC. Los resúmenes no incluyen secretos ni reconstruyen todas las versiones históricas; no hay escritura libre de eventos.")
    @GetMapping
    public List<AuditoriaResponse> listar(@ParameterObject @Valid @ModelAttribute FiltroAuditoriaRequest filtro) {
        return this.mapper.toResponse(this.service.listar(this.mapper.convert(filtro)));
    }
}
