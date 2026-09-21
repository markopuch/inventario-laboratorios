package com.utec.inventario.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.domain.Area;
import com.utec.inventario.dto.request.CreateAreaRequest;
import com.utec.inventario.dto.request.UpdateAreaRequest;
import com.utec.inventario.dto.response.AreaResponse;
import com.utec.inventario.mapper.AreaMapper;
import com.utec.inventario.service.AreaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;
    private final AreaMapper mapper;

    @Autowired
    public AreaController(AreaService areaService, AreaMapper mapper) {
        this.areaService = areaService;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AreaResponse> listarAreas() {
        return this.mapper.toResponse(this.areaService.listarAreas());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AreaResponse obtenerArea(@Positive @PathVariable("id") Integer id) {
        return this.mapper.toResponse(this.areaService.obtenerArea(id));
    }

    @PostMapping
    public ResponseEntity<AreaResponse> crearArea(
            @Valid @RequestBody CreateAreaRequest request) {
        Area area = this.mapper.convert(request);
        Area creado = this.areaService.crearArea(area);
        AreaResponse response = this.mapper.toResponse(creado);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AreaResponse actualizarArea(@Positive @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateAreaRequest request) {
        Area cambios = this.mapper.convert(request);
        Area actualizado = this.areaService.actualizarArea(id, cambios);
        return this.mapper.toResponse(actualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarArea(@Positive @PathVariable("id") Integer id) {
        this.areaService.eliminarArea(id);
    }
}
