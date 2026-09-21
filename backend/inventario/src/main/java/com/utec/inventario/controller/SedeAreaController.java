package com.utec.inventario.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.dto.response.AreaResponse;
import com.utec.inventario.mapper.AreaMapper;
import com.utec.inventario.service.AreaService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/sedes/{idSede}/areas")
public class SedeAreaController {

    private final AreaService areaService;
    private final AreaMapper mapper;

    @Autowired
    public SedeAreaController(AreaService areaService, AreaMapper mapper) {
        this.areaService = areaService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AreaResponse> listarPorSede(
            @Positive @PathVariable("idSede") Integer idSede) {
        return this.mapper.toResponse(this.areaService.listarPorSede(idSede));
    }
}
