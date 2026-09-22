package com.utec.inventario.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.entity.CategoriaEntity;
import com.utec.inventario.entity.SubcategoriaEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.SubcategoriaMapper;
import com.utec.inventario.repository.CategoriaRepository;
import com.utec.inventario.repository.SubcategoriaRepository;
import com.utec.inventario.repository.EquipoRepository;

@Service
@Transactional(readOnly = true)
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaMapper mapper;
    private final EquipoRepository equipoRepository;

    @Autowired
    public SubcategoriaService(SubcategoriaRepository subcategoriaRepository,
            CategoriaRepository categoriaRepository, SubcategoriaMapper mapper,
            EquipoRepository equipoRepository) {
        this.subcategoriaRepository = subcategoriaRepository;
        this.categoriaRepository = categoriaRepository;
        this.mapper = mapper;
        this.equipoRepository = equipoRepository;
    }

    public List<Subcategoria> listarSubcategorias() {
        return this.mapper.convert(this.subcategoriaRepository.findAllByActivoTrueOrderByIdSubcategoriaAsc());
    }

    public Subcategoria obtenerSubcategoria(Integer id) {
        SubcategoriaEntity subcategoria = this.subcategoriaRepository.findByIdSubcategoriaAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una subcategoría activa con el ID " + id + "."));
        return this.mapper.convert(subcategoria);
    }

    public List<Subcategoria> listarPorCategoria(Integer idCategoria) {
        this.categoriaRepository.findByIdCategoriaAndActivoTrue(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una categoría activa con el ID " + idCategoria + "."));
        return this.mapper.convert(this.subcategoriaRepository
                .findAllByCategoria_IdCategoriaAndActivoTrueOrderByIdSubcategoriaAsc(idCategoria));
    }

    @Transactional
    public Subcategoria crearSubcategoria(Subcategoria subcategoria) {
        CategoriaEntity categoria = this.buscarCategoriaActivaParaRelacionar(subcategoria.getCategoria().getId());
        String nombre = subcategoria.getNombre().trim();
        if (this.subcategoriaRepository.existsByCategoria_IdCategoriaAndNombreIgnoreCase(
                categoria.getIdCategoria(), nombre)) {
            throw new ConflictException("Ya existe una subcategoría con ese nombre en la categoría seleccionada.");
        }

        subcategoria.setId(null);
        subcategoria.setNombre(nombre);
        subcategoria.setDescripcion(this.normalizarDescripcion(subcategoria.getDescripcion()));
        subcategoria.setActivo(true);
        subcategoria.setFechaCreacion(null);

        SubcategoriaEntity nuevaSubcategoria = this.mapper.toEntity(subcategoria);
        nuevaSubcategoria.setCategoria(categoria);
        SubcategoriaEntity subcategoriaGuardada = this.subcategoriaRepository.saveAndFlush(nuevaSubcategoria);
        return this.mapper.convert(subcategoriaGuardada);
    }

    @Transactional
    public Subcategoria actualizarSubcategoria(Integer id, Subcategoria cambios) {
        SubcategoriaEntity subcategoria = this.buscarSubcategoriaActivaParaModificar(id);
        CategoriaEntity categoria = this.buscarCategoriaActivaParaRelacionar(cambios.getCategoria().getId());
        String nombre = cambios.getNombre().trim();
        if (this.subcategoriaRepository.existsByCategoria_IdCategoriaAndNombreIgnoreCaseAndIdSubcategoriaNot(
                categoria.getIdCategoria(), nombre, id)) {
            throw new ConflictException("Ya existe una subcategoría con ese nombre en la categoría seleccionada.");
        }

        cambios.setNombre(nombre);
        cambios.setDescripcion(this.normalizarDescripcion(cambios.getDescripcion()));

        SubcategoriaEntity subcategoriaActualizada = this.mapper.copy(subcategoria, cambios);
        subcategoriaActualizada.setCategoria(categoria);
        SubcategoriaEntity subcategoriaGuardada = this.subcategoriaRepository.saveAndFlush(subcategoriaActualizada);
        return this.mapper.convert(subcategoriaGuardada);
    }

    @Transactional
    public void eliminarSubcategoria(Integer id) {
        SubcategoriaEntity subcategoria = this.buscarSubcategoriaActivaParaModificar(id);
        if (this.equipoRepository.existsBySubcategoria_IdSubcategoriaAndEstadoNot(id, EstadoEquipo.BAJA)) {
            throw new ConflictException(
                    "No se puede desactivar la subcategoría porque contiene equipos no dados de baja.");
        }
        subcategoria.setActivo(false);
        this.subcategoriaRepository.saveAndFlush(subcategoria);
    }

    private SubcategoriaEntity buscarSubcategoriaActivaParaModificar(Integer id) {
        return this.subcategoriaRepository.findForUpdateByIdSubcategoriaAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una subcategoría activa con el ID " + id + "."));
    }

    private CategoriaEntity buscarCategoriaActivaParaRelacionar(Integer idCategoria) {
        // Comparte el bloqueo usado por DELETE de Categoria hasta finalizar esta transacción.
        return this.categoriaRepository.findForUpdateByIdCategoriaAndActivoTrue(idCategoria)
                .orElseThrow(() -> {
                    if (this.categoriaRepository.existsById(idCategoria)) {
                        return new ConflictException("La categoría seleccionada está inactiva.");
                    }
                    return new ResourceNotFoundException("No existe una categoría con el ID " + idCategoria + ".");
                });
    }

    private String normalizarDescripcion(String descripcion) {
        return descripcion == null ? null : descripcion.trim();
    }
}
