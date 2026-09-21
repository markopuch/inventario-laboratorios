package com.utec.inventario.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.domain.UsuarioLaboratorio;
import com.utec.inventario.domain.UsuarioLaboratorios;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioId;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class UsuarioLaboratorioMapperTest {
    private final UsuarioLaboratorioMapper mapper = Mappers.getMapper(UsuarioLaboratorioMapper.class);

    @Test
    void entitySeConvierteSinLeerUsuarioJpaNiSusCredenciales() {
        UsuarioEntity usuarioJpa = mock(UsuarioEntity.class);
        Usuario publicUser = Usuario.builder().id(2).userName("gestor").nombre("Nombre")
                .apellido("Apellido").rol("GESTOR").activo(true).build();
        OffsetDateTime originalDate = OffsetDateTime.parse("2026-01-10T15:00:00Z");
        LaboratorioEntity laboratorio = LaboratorioEntity.builder().idLaboratorio(7).codigo("L700")
                .nombre("Robótica").activo(true).build();
        UsuarioLaboratorioEntity entity = UsuarioLaboratorioEntity.builder()
                .id(new UsuarioLaboratorioId(2, 7)).usuario(usuarioJpa).laboratorio(laboratorio)
                .activo(true).fechaAsignacion(originalDate).build();
        UsuarioLaboratorio domain = mapper.convert(entity, publicUser);
        assertEquals(2, domain.getUsuario().getId());
        assertEquals(7, domain.getLaboratorio().getId());
        assertEquals("L700", domain.getLaboratorio().getCodigo());
        assertEquals(originalDate, domain.getFechaAsignacion());
        assertTrue(domain.isActivo());
        verifyNoInteractions(usuarioJpa);
    }

    @Test
    void respuestasDistinguenAsignacionYAlcanceYExponenSoloDatosPublicos() {
        Usuario user = Usuario.builder().id(2).userName("gestor").nombre("Nombre").apellido("Apellido")
                .rol("GESTOR").email("privado@example.invalid").activo(true).build();
        Laboratorio lab = Laboratorio.builder().id(7).codigo("L700").nombre("Robótica").activo(true)
                .ubicacion("Detalle interno").build();
        var assignments = mapper.toResponse(UsuarioLaboratorios.builder().usuario(user).laboratorios(List.of(lab)).build());
        var scope = mapper.toResponse(AlcanceLaboratorios.builder().alcanceGlobal(false).laboratorios(List.of(lab)).build());
        ObjectMapper json = new ObjectMapper();
        JsonNode response = json.readTree(json.writeValueAsString(assignments));
        assertEquals(5, response.path("usuario").size());
        assertEquals(2, response.path("usuario").path("id").asInt());
        assertEquals("GESTOR", response.path("usuario").path("rol").asString());
        assertFalse(response.path("usuario").has("email"));
        assertEquals(3, response.path("laboratorios").get(0).size());
        assertEquals(7, response.path("laboratorios").get(0).path("id").asInt());
        assertFalse(json.writeValueAsString(assignments).toLowerCase().contains("password"));
        JsonNode effective = json.readTree(json.writeValueAsString(scope));
        assertFalse(effective.path("alcanceGlobal").asBoolean());
        assertEquals(response.path("laboratorios"), effective.path("laboratorios"));
        assertFalse(effective.has("usuario"));
    }
}
