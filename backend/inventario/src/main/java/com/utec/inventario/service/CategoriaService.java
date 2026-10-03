package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.domain.AccionAuditoria;
import com.utec.inventario.entity.CategoriaEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.repository.CategoriaRepository;
import com.utec.inventario.repository.SubcategoriaRepository;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final AuditoriaService auditoriaService;

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper mapper;
    private final SubcategoriaRepository subcategoriaRepository;

    @Autowired
    public CategoriaService(CategoriaRepository categoriaRepository, CategoriaMapper mapper,
            SubcategoriaRepository subcategoriaRepository, AuditoriaService auditoriaService) {
        this.categoriaRepository = categoriaRepository;
        this.mapper = mapper;
        this.subcategoriaRepository = subcategoriaRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Categoria> listarCategorias() {
        return this.mapper.convert(this.categoriaRepository.findAllByActivoTrueOrderByIdCategoriaAsc());
    }

    public List<Categoria> listarCategoriasAdmin(Boolean activo) {
        return this.mapper.convert(activo == null
                ? this.categoriaRepository.findAllByOrderByIdCategoriaAsc()
                : this.categoriaRepository.findAllByActivoOrderByIdCategoriaAsc(activo));
    }

    @Transactional
    public Categoria cambiarEstadoCategoria(Integer id, boolean activo) {
        CategoriaEntity categoria = this.categoriaRepository.findForUpdateByIdCategoria(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una categoría con el ID " + id + "."));
        if (categoria.isActivo() == activo) {
            return this.mapper.convert(categoria);
        }
        if (!activo) {
            this.validarDesactivacion(id);
        }
        categoria.setActivo(activo);
        CategoriaEntity guardado = this.categoriaRepository.saveAndFlush(categoria);
        this.auditoriaService.registrar(activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR,
                "categoria", id, "activo: " + !activo + " -> " + activo);
        return this.mapper.convert(guardado);
    }

    public Categoria obtenerCategoria(Integer id) {
        return this.mapper.convert(this.buscarCategoriaActiva(id));
    }

    @Transactional
    public Categoria crearCategoria(Categoria categoria) {
        String nombre = categoria.getNombre().trim();
        if (this.categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una categoría con ese nombre.");
        }

        categoria.setId(null);
        categoria.setNombre(nombre);
        categoria.setDescripcion(this.normalizarDescripcion(categoria.getDescripcion()));
        categoria.setActivo(true);
        categoria.setFechaCreacion(null);

        CategoriaEntity nuevaCategoria = this.mapper.toEntity(categoria);
        CategoriaEntity categoriaGuardada = this.categoriaRepository.saveAndFlush(nuevaCategoria);
        this.auditoriaService.registrar(AccionAuditoria.CREAR, "categoria", categoriaGuardada.getIdCategoria(),
                "Creación de catálogo; activo: true");
        return this.mapper.convert(categoriaGuardada);
    }

    @Transactional
    public Categoria actualizarCategoria(Integer id, Categoria cambios) {
        CategoriaEntity categoria = this.buscarCategoriaActivaParaModificar(id);
        String nombre = cambios.getNombre().trim();
        if (this.categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(nombre, id)) {
            throw new ConflictException("Ya existe una categoría con ese nombre.");
        }

        cambios.setNombre(nombre);
        cambios.setDescripcion(this.normalizarDescripcion(cambios.getDescripcion()));

        CategoriaEntity categoriaActualizada = this.mapper.copy(categoria, cambios);
        CategoriaEntity categoriaGuardada = this.categoriaRepository.saveAndFlush(categoriaActualizada);
        this.auditoriaService.registrar(AccionAuditoria.EDITAR, "categoria", id,
                "Actualización de nombre, descripcion");
        return this.mapper.convert(categoriaGuardada);
    }

    @Transactional
    public void eliminarCategoria(Integer id) {
        CategoriaEntity categoria = this.buscarCategoriaActivaParaModificar(id);
        this.validarDesactivacion(id);
        categoria.setActivo(false);
        this.categoriaRepository.saveAndFlush(categoria);
        this.auditoriaService.registrar(AccionAuditoria.DESACTIVAR, "categoria", id, "activo: true -> false");
    }

    private void validarDesactivacion(Integer id) {
        if (this.subcategoriaRepository.existsByCategoria_IdCategoriaAndActivoTrue(id)) {
            throw new ConflictException("No se puede desactivar la categoría porque contiene subcategorías activas.");
        }
    }

    private CategoriaEntity buscarCategoriaActiva(Integer id) {
        return this.categoriaRepository.findByIdCategoriaAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una categoría activa con el ID " + id + "."));
    }

    private String normalizarDescripcion(String descripcion) {
        return descripcion == null ? null : descripcion.trim();
    }

    private CategoriaEntity buscarCategoriaActivaParaModificar(Integer id) {
        return this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una categoría activa con el ID " + id + "."));
    }
}
