package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.dto.response.CategoriaResponse;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.service.CategoriaService;
import com.utec.inventario.dto.response.SubcategoriaResponse;
import com.utec.inventario.mapper.SubcategoriaMapper;
import com.utec.inventario.service.SubcategoriaService;
import com.utec.inventario.dto.response.SedeResponse;
import com.utec.inventario.mapper.SedeMapper;
import com.utec.inventario.service.SedeService;
import com.utec.inventario.dto.response.AreaResponse;
import com.utec.inventario.mapper.AreaMapper;
import com.utec.inventario.service.AreaService;
import com.utec.inventario.dto.response.LaboratorioResponse;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.service.LaboratorioService;

@Tag(name = "Catálogos administrativos", description = "Consulta exclusiva de ADMIN, con filtro opcional de actividad.")
@RestController
@RequestMapping("/api/admin")
public class CatalogoAdminController {

    private final CategoriaService categoriaService;
    private final CategoriaMapper categoriaMapper;
    private final SubcategoriaService subcategoriaService;
    private final SubcategoriaMapper subcategoriaMapper;
    private final SedeService sedeService;
    private final SedeMapper sedeMapper;
    private final AreaService areaService;
    private final AreaMapper areaMapper;
    private final LaboratorioService laboratorioService;
    private final LaboratorioMapper laboratorioMapper;

    @Autowired
    public CatalogoAdminController(
            CategoriaService categoriaService, CategoriaMapper categoriaMapper,
            SubcategoriaService subcategoriaService, SubcategoriaMapper subcategoriaMapper,
            SedeService sedeService, SedeMapper sedeMapper,
            AreaService areaService, AreaMapper areaMapper,
            LaboratorioService laboratorioService, LaboratorioMapper laboratorioMapper) {
        this.categoriaService = categoriaService;
        this.categoriaMapper = categoriaMapper;
        this.subcategoriaService = subcategoriaService;
        this.subcategoriaMapper = subcategoriaMapper;
        this.sedeService = sedeService;
        this.sedeMapper = sedeMapper;
        this.areaService = areaService;
        this.areaMapper = areaMapper;
        this.laboratorioService = laboratorioService;
        this.laboratorioMapper = laboratorioMapper;
    }

    @Operation(summary = "Listar categorías administrativas",
            description = "Solo ADMIN, alcance global. Sin activo devuelve activas e inactivas; activo=true o false filtra explícitamente.")
    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias(@RequestParam(required = false) Boolean activo) {
        return this.categoriaMapper.toResponse(this.categoriaService.listarCategoriasAdmin(activo));
    }

    @Operation(summary = "Listar subcategorías administrativas",
            description = "Solo ADMIN, alcance global. Incluye inactivas si se omite activo y permite filtrar por actividad.")
    @GetMapping("/subcategorias")
    public List<SubcategoriaResponse> listarSubcategorias(@RequestParam(required = false) Boolean activo) {
        return this.subcategoriaMapper.toResponse(this.subcategoriaService.listarSubcategoriasAdmin(activo));
    }

    @Operation(summary = "Listar sedes administrativas",
            description = "Solo ADMIN, alcance global. Incluye inactivas si se omite activo y permite filtrar por actividad.")
    @GetMapping("/sedes")
    public List<SedeResponse> listarSedes(@RequestParam(required = false) Boolean activo) {
        return this.sedeMapper.toResponse(this.sedeService.listarSedesAdmin(activo));
    }

    @Operation(summary = "Listar áreas administrativas",
            description = "Solo ADMIN, alcance global. Incluye inactivas si se omite activo y permite filtrar por actividad.")
    @GetMapping("/areas")
    public List<AreaResponse> listarAreas(@RequestParam(required = false) Boolean activo) {
        return this.areaMapper.toResponse(this.areaService.listarAreasAdmin(activo));
    }

    @Operation(summary = "Listar laboratorios administrativos",
            description = "Solo ADMIN, alcance global. Filtra por actividad lógica; estadoOperativo es un dato independiente. Sin activo incluye activos e inactivos.")
    @GetMapping("/laboratorios")
    public List<LaboratorioResponse> listarLaboratorios(@RequestParam(required = false) Boolean activo) {
        return this.laboratorioMapper.toResponse(this.laboratorioService.listarLaboratoriosAdmin(activo));
    }
}

