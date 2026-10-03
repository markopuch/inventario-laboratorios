package com.utec.inventario.controller;

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

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias(@RequestParam(required = false) Boolean activo) {
        return this.categoriaMapper.toResponse(this.categoriaService.listarCategoriasAdmin(activo));
    }

    @GetMapping("/subcategorias")
    public List<SubcategoriaResponse> listarSubcategorias(@RequestParam(required = false) Boolean activo) {
        return this.subcategoriaMapper.toResponse(this.subcategoriaService.listarSubcategoriasAdmin(activo));
    }

    @GetMapping("/sedes")
    public List<SedeResponse> listarSedes(@RequestParam(required = false) Boolean activo) {
        return this.sedeMapper.toResponse(this.sedeService.listarSedesAdmin(activo));
    }

    @GetMapping("/areas")
    public List<AreaResponse> listarAreas(@RequestParam(required = false) Boolean activo) {
        return this.areaMapper.toResponse(this.areaService.listarAreasAdmin(activo));
    }

    @GetMapping("/laboratorios")
    public List<LaboratorioResponse> listarLaboratorios(@RequestParam(required = false) Boolean activo) {
        return this.laboratorioMapper.toResponse(this.laboratorioService.listarLaboratoriosAdmin(activo));
    }
}

