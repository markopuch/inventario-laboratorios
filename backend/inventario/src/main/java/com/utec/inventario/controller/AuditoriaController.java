package com.utec.inventario.controller;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.utec.inventario.dto.request.FiltroAuditoriaRequest;
import com.utec.inventario.dto.response.AuditoriaResponse;
import com.utec.inventario.mapper.AuditoriaMapper;
import com.utec.inventario.service.AuditoriaService;
import jakarta.validation.Valid;

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
    @GetMapping
    public List<AuditoriaResponse> listar(@Valid @ModelAttribute FiltroAuditoriaRequest filtro) {
        return this.mapper.toResponse(this.service.listar(this.mapper.convert(filtro)));
    }
}
