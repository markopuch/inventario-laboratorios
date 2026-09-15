package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.Categoria;
import com.utec.inventario.dto.request.CreateCategoriaRequest;
import com.utec.inventario.dto.request.UpdateCategoriaRequest;
import com.utec.inventario.entity.CategoriaEntity;

class CategoriaMapperTest {

    private final CategoriaMapper mapper = Mappers.getMapper(CategoriaMapper.class);

    @Test
    void copiarCambiosConservaCamposDelServidorYPermiteBorrarDescripcion() {
        OffsetDateTime fechaOriginal = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        CategoriaEntity entity = CategoriaEntity.builder()
                .idCategoria(7)
                .nombre("Instrumentos")
                .descripcion("Descripción anterior")
                .activo(true)
                .fechaCreacion(fechaOriginal)
                .build();
        Categoria cambios = Categoria.builder()
                .id(999)
                .nombre("Instrumentación")
                .descripcion(null)
                .activo(false)
                .fechaCreacion(fechaOriginal.plusDays(1))
                .build();

        CategoriaEntity resultado = this.mapper.copy(entity, cambios);

        assertAll(
                () -> assertSame(entity, resultado),
                () -> assertEquals(7, resultado.getIdCategoria()),
                () -> assertTrue(resultado.isActivo()),
                () -> assertEquals(fechaOriginal, resultado.getFechaCreacion()),
                () -> assertEquals("Instrumentación", resultado.getNombre()),
                () -> assertNull(resultado.getDescripcion()));
    }

    @Test
    void solicitudesCreanDominioSinIdentidadEstadoNiFechaAsignados() {
        CreateCategoriaRequest crear = new CreateCategoriaRequest("Robótica", "Sensores");
        UpdateCategoriaRequest actualizar = new UpdateCategoriaRequest("Robótica", "Sensores");

        List<Categoria> categorias = List.of(this.mapper.convert(crear), this.mapper.convert(actualizar));

        for (Categoria categoria : categorias) {
            assertAll(
                    () -> assertEquals("Robótica", categoria.getNombre()),
                    () -> assertEquals("Sensores", categoria.getDescripcion()),
                    () -> assertNull(categoria.getId()),
                    () -> assertFalse(categoria.isActivo()),
                    () -> assertNull(categoria.getFechaCreacion()));
        }
    }
}
