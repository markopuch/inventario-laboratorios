package com.utec.inventario.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.CategoriaMapper;
import com.utec.inventario.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository repository;
    private final CategoriaMapper mapper;

    public List<Categoria> listarCategorias() {
        return repository.findAllByActivoTrueOrderByIdCategoriaAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    public Categoria obtenerCategoria(Integer id) {
        return buscarCategoriaActiva(id);
    }

    @Transactional
    public Categoria crearCategoria(Categoria categoria) {
        String nombre = categoria.getNombre().trim();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una categoría con ese nombre.");
        }

        categoria.setId(null);
        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizarDescripcion(categoria.getDescripcion()));
        categoria.setActivo(true);
        categoria.setFechaCreacion(null);

        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(categoria)));
    }

    @Transactional
    public Categoria actualizarCategoria(Integer id, Categoria cambios) {
        Categoria categoria = buscarCategoriaActiva(id);
        String nombre = cambios.getNombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdCategoriaNot(nombre, id)) {
            throw new ConflictException("Ya existe una categoría con ese nombre.");
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizarDescripcion(cambios.getDescripcion()));

        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(categoria)));
    }

    @Transactional
    public void eliminarCategoria(Integer id) {
        Categoria categoria = buscarCategoriaActiva(id);
        categoria.setActivo(false);
        repository.saveAndFlush(mapper.toEntity(categoria));
    }

    private Categoria buscarCategoriaActiva(Integer id) {
        return repository.findByIdCategoriaAndActivoTrue(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una categoría activa con el ID " + id + "."));
    }

    private String normalizarDescripcion(String descripcion) {
        return descripcion == null ? null : descripcion.trim();
    }
}
