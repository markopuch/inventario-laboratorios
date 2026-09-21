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
import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.dto.request.CreateSubcategoriaRequest;
import com.utec.inventario.dto.request.UpdateSubcategoriaRequest;
import com.utec.inventario.dto.response.SubcategoriaResponse;
import com.utec.inventario.entity.CategoriaEntity;
import com.utec.inventario.entity.SubcategoriaEntity;

class SubcategoriaMapperTest {

    private static final OffsetDateTime FECHA_CREACION = OffsetDateTime.parse("2026-01-10T15:00:00Z");
    private final SubcategoriaMapper mapper = Mappers.getMapper(SubcategoriaMapper.class);

    @Test
    void solicitudesCreanDominioConReferenciaAlPadreSinCamposDelServidor() {
        CreateSubcategoriaRequest crear = new CreateSubcategoriaRequest("Sensores", "Medición", 3);
        UpdateSubcategoriaRequest actualizar = new UpdateSubcategoriaRequest("Sensores", "Medición", 3);

        List<Subcategoria> resultados = List.of(this.mapper.convert(crear), this.mapper.convert(actualizar));

        for (Subcategoria subcategoria : resultados) {
            assertAll(
                    () -> assertEquals("Sensores", subcategoria.getNombre()),
                    () -> assertEquals("Medición", subcategoria.getDescripcion()),
                    () -> assertNull(subcategoria.getId()),
                    () -> assertFalse(subcategoria.isActivo()),
                    () -> assertNull(subcategoria.getFechaCreacion()),
                    () -> assertEquals(3, subcategoria.getCategoria().getId()),
                    () -> assertNull(subcategoria.getCategoria().getNombre()));
        }
    }

    @Test
    void copiarCambiosPreservaIdentidadEstadoFechaYPadreHastaQueElServicioLoResuelva() {
        CategoriaEntity padreOriginal = categoriaEntity();
        SubcategoriaEntity entity = SubcategoriaEntity.builder()
                .idSubcategoria(7).nombre("Sensores").descripcion("Descripción anterior")
                .activo(true).fechaCreacion(FECHA_CREACION).categoria(padreOriginal).build();
        Subcategoria cambios = Subcategoria.builder()
                .id(999).nombre("Sensores de posición").descripcion(null)
                .activo(false).fechaCreacion(FECHA_CREACION.plusDays(1))
                .categoria(Categoria.builder().id(4).nombre("Padre no validado").build()).build();

        SubcategoriaEntity resultado = this.mapper.copy(entity, cambios);

        assertAll(
                () -> assertSame(entity, resultado),
                () -> assertEquals(7, resultado.getIdSubcategoria()),
                () -> assertTrue(resultado.isActivo()),
                () -> assertEquals(FECHA_CREACION, resultado.getFechaCreacion()),
                () -> assertEquals("Sensores de posición", resultado.getNombre()),
                () -> assertNull(resultado.getDescripcion()),
                () -> assertSame(padreOriginal, resultado.getCategoria()));
    }

    @Test
    void convertirAEntityDejaLaRelacionPersistenteParaElServicio() {
        Subcategoria dominio = Subcategoria.builder()
                .id(7).nombre("Sensores").descripcion("Medición").activo(true)
                .fechaCreacion(FECHA_CREACION)
                .categoria(Categoria.builder().id(3).nombre("Electrónica").build()).build();

        SubcategoriaEntity entity = this.mapper.toEntity(dominio);

        assertAll(
                () -> assertEquals(7, entity.getIdSubcategoria()),
                () -> assertEquals("Sensores", entity.getNombre()),
                () -> assertEquals("Medición", entity.getDescripcion()),
                () -> assertTrue(entity.isActivo()),
                () -> assertEquals(FECHA_CREACION, entity.getFechaCreacion()),
                () -> assertNull(entity.getCategoria()));
    }

    @Test
    void entidadYListaSeConviertenARespuestaConResumenDelPadre() {
        SubcategoriaEntity entity = SubcategoriaEntity.builder()
                .idSubcategoria(7).nombre("Sensores").descripcion("Medición")
                .activo(true).fechaCreacion(FECHA_CREACION).categoria(categoriaEntity()).build();

        List<Subcategoria> dominios = this.mapper.convert(List.of(entity));
        List<SubcategoriaResponse> respuestas = this.mapper.toResponse(dominios);
        Subcategoria dominio = dominios.getFirst();
        SubcategoriaResponse respuesta = respuestas.getFirst();

        assertAll(
                () -> assertEquals(1, dominios.size()),
                () -> assertEquals(1, respuestas.size()),
                () -> assertEquals(7, dominio.getId()),
                () -> assertEquals(3, dominio.getCategoria().getId()),
                () -> assertEquals("Electrónica", dominio.getCategoria().getNombre()),
                () -> assertEquals(7, respuesta.getId()),
                () -> assertEquals("Sensores", respuesta.getNombre()),
                () -> assertEquals("Medición", respuesta.getDescripcion()),
                () -> assertTrue(respuesta.isActivo()),
                () -> assertEquals(FECHA_CREACION, respuesta.getFechaCreacion()),
                () -> assertEquals(3, respuesta.getCategoria().getId()),
                () -> assertEquals("Electrónica", respuesta.getCategoria().getNombre()));
    }

    private CategoriaEntity categoriaEntity() {
        return CategoriaEntity.builder().idCategoria(3).nombre("Electrónica")
                .descripcion("Descripción del padre").activo(true).fechaCreacion(FECHA_CREACION).build();
    }
}
