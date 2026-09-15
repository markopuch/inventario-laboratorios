package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.entity.CategoriaEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.repository.CategoriaRepository;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper mapper;

    @Autowired
    public CategoriaService(CategoriaRepository categoriaRepository, CategoriaMapper mapper) {
        this.categoriaRepository = categoriaRepository;
        this.mapper = mapper;
    }

    public List<Categoria> listarCategorias() {
        return this.mapper.convert(this.categoriaRepository.findAllByActivoTrueOrderByIdCategoriaAsc());
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
        return this.mapper.convert(categoriaGuardada);
    }

    @Transactional
    public void eliminarCategoria(Integer id) {
        CategoriaEntity categoria = this.buscarCategoriaActivaParaModificar(id);
        categoria.setActivo(false);
        this.categoriaRepository.saveAndFlush(categoria);
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
