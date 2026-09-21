package com.utec.inventario.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.dto.response.LaboratorioResponse;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.service.LaboratorioService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/areas/{idArea}/laboratorios")
public class AreaLaboratorioController {

    private final LaboratorioService laboratorioService;
    private final LaboratorioMapper mapper;

    @Autowired
    public AreaLaboratorioController(LaboratorioService laboratorioService, LaboratorioMapper mapper) {
        this.laboratorioService = laboratorioService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<LaboratorioResponse> listarPorArea(
            @Positive @PathVariable("idArea") Integer idArea) {
        return this.mapper.toResponse(this.laboratorioService.listarPorArea(idArea));
    }
}
