package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Subcategoria;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.SubcategoriaEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.repository.EquipoRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.SubcategoriaRepository;
import com.utec.inventario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {
    private static final OffsetDateTime ORIGINAL = OffsetDateTime.parse("2026-01-10T15:00:00Z");
    @Mock private EquipoRepository equipos;
    @Mock private SubcategoriaRepository subcategorias;
    @Mock private LaboratorioRepository laboratorios;
    @Mock private UsuarioRepository usuarios;
    @Mock private AlcanceLaboratorioService alcance;
    private EquipoService service;

    @BeforeEach
    void preparar() {
        service = new EquipoService(equipos, subcategorias, laboratorios, usuarios, alcance, Mappers.getMapper(EquipoMapper.class));
    }

    @Test
    void listaRestringidaEnviaAlcanceYFiltrosAlRepositorioYSinAlcanceNoConsultaEquipos() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("GESTOR")));
        when(alcance.obtenerAlcanceEfectivo(1)).thenReturn(
                AlcanceLaboratorios.builder().alcanceGlobal(false).laboratorios(List.of(Laboratorio.builder().id(3).build())).build(),
                AlcanceLaboratorios.builder().alcanceGlobal(false).laboratorios(List.of()).build());
        when(equipos.findAllFiltradosEnLaboratorios(EstadoEquipo.MANTENIMIENTO, 3, 2, true, List.of(3)))
                .thenReturn(List.of(entity()));
        assertEquals(1, service.listarEquipos(1, EstadoEquipo.MANTENIMIENTO, 3, 2, true).size());
        verify(alcance).validarAccesoLaboratorio(1, 3);
        verify(equipos).findAllFiltradosEnLaboratorios(EstadoEquipo.MANTENIMIENTO, 3, 2, true, List.of(3));
        assertTrue(service.listarEquipos(1, null, null, null, null).isEmpty());
        verifyNoMoreInteractions(equipos);
        verify(equipos, never()).findAll();
    }

    @Test
    void adminConservaHistoricoConPadresInactivosYResuelveResponsablesEnUnaProyeccionPublica() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("ADMIN")));
        EquipoEntity first = entity();
        first.setEstado(EstadoEquipo.BAJA);
        first.getLaboratorio().setActivo(false);
        UsuarioEntity responsable = mock(UsuarioEntity.class);
        when(responsable.getIdUsuario()).thenReturn(4);
        first.setResponsable(responsable);
        EquipoEntity second = entity();
        second.setIdEquipo(8);
        second.setResponsable(responsable);
        when(equipos.findAllFiltrados(null, null, null, null)).thenReturn(List.of(first, second));
        when(usuarios.findPublicByIdUsuarioIn(List.of(4))).thenReturn(List.of(Usuario.builder().id(4).userName("responsable").build()));
        List<Equipo> result = service.listarEquipos(1, null, null, null, null);
        assertEquals(2, result.size());
        assertEquals(EstadoEquipo.BAJA, result.getFirst().getEstado());
        assertEquals(4, result.getFirst().getResponsable().getId());
        verify(usuarios).findPublicByIdUsuarioIn(List.of(4));
        verify(responsable, never()).getPasswordHash();
        verifyNoInteractions(alcance, laboratorios, subcategorias);
    }

    @Test
    void crearBloqueaActorYPadresEnOrdenNormalizaOpcionalesYAsignaEntidadesValidadas() {
        actorEscritura("ADMIN");
        SubcategoriaEntity sub = subcategoria(2);
        LaboratorioEntity lab = laboratorio(3);
        when(subcategorias.findForUpdateByIdSubcategoriaAndActivoTrue(2)).thenReturn(Optional.of(sub));
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(3)).thenReturn(Optional.of(lab));
        when(equipos.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        Equipo input = input();
        input.setId(999);
        input.setCodigoInterno("  EQ-1  ");
        input.setNombre("  Osciloscopio  ");
        input.setSerieUtec("   ");
        input.setNumeroSerie("");
        input.setMarca("  Rigol  ");
        input.setComentario("  Comentario  ");
        input.setFechaCreacion(ORIGINAL);
        input.setFechaActualizacion(ORIGINAL);
        Equipo result = service.crearEquipo(1, input);
        assertNull(result.getId());
        assertNull(result.getFechaCreacion());
        assertNull(result.getFechaActualizacion());
        assertEquals("EQ-1", result.getCodigoInterno());
        assertEquals("Osciloscopio", result.getNombre());
        assertEquals("Rigol", result.getMarca());
        assertEquals("Comentario", result.getComentario());
        assertNull(result.getSerieUtec());
        assertNull(result.getNumeroSerie());
        assertNull(result.getResponsable());
        var ordered = inOrder(usuarios, subcategorias, laboratorios, equipos);
        ordered.verify(usuarios).findIdForShareByIdUsuario(1);
        ordered.verify(usuarios).findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1);
        ordered.verify(subcategorias).findForUpdateByIdSubcategoriaAndActivoTrue(2);
        ordered.verify(laboratorios).findForUpdateByIdLaboratorioAndActivoTrue(3);
        ordered.verify(equipos).existsByCodigoInterno("EQ-1");
        verify(equipos, never()).existsBySerieUtec(any());
        verify(equipos, never()).existsByNumeroSerie(any());
        verifyNoInteractions(alcance);
    }

    @Test
    void serviceRechazaEscrituraLectorYGestorFueraDeAlcanceAntesDePersistir() {
        actorEscritura("LECTOR");
        assertThrows(AccessDeniedException.class, () -> service.crearEquipo(1, input()));
        assertThrows(AccessDeniedException.class, () -> service.actualizarEquipo(1, 7, input()));
        assertThrows(AccessDeniedException.class, () -> service.eliminarEquipo(1, 7));
        verifyNoInteractions(equipos, subcategorias, laboratorios);
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("GESTOR")));
        when(subcategorias.findForUpdateByIdSubcategoriaAndActivoTrue(2)).thenReturn(Optional.of(subcategoria(2)));
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(3)).thenReturn(Optional.of(laboratorio(3)));
        doThrow(new AccessDeniedException("Fuera de alcance")).when(alcance).validarAccesoLaboratorio(1, 3);
        assertThrows(AccessDeniedException.class, () -> service.crearEquipo(1, input()));
        verify(equipos, never()).saveAndFlush(any());
    }

    @Test
    void actualizarIgnoraInmutablesDelDominioLimpiaOpcionalesYActualizaFecha() {
        actorEscritura("ADMIN");
        EquipoEntity entity = entity();
        LaboratorioEntity originalLab = entity.getLaboratorio();
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(entity));
        when(subcategorias.findForUpdateByIdSubcategoriaAndActivoTrue(20)).thenReturn(Optional.of(subcategoria(20)));
        when(laboratorios.findForUpdateByIdLaboratorio(3)).thenReturn(Optional.of(originalLab));
        when(equipos.saveAndFlush(entity)).thenReturn(entity);
        Equipo change = input();
        change.setId(999);
        change.setCodigoInterno("CAMBIO");
        change.setLaboratorio(Laboratorio.builder().id(999).build());
        change.setSubcategoria(Subcategoria.builder().id(20).build());
        change.setNombre("  Editado  ");
        change.setFechaCreacion(ORIGINAL.plusDays(1));
        change.setFechaActualizacion(ORIGINAL.plusDays(1));
        Equipo result = service.actualizarEquipo(1, 7, change);
        assertEquals(7, result.getId());
        assertEquals("EQ-1", result.getCodigoInterno());
        assertEquals(ORIGINAL, result.getFechaCreacion());
        assertTrue(result.getFechaActualizacion().isAfter(ORIGINAL));
        assertSame(originalLab, entity.getLaboratorio());
        assertEquals(20, result.getSubcategoria().getId());
        assertNull(result.getMarca());
        assertEquals("Editado", result.getNombre());
        verify(equipos, never()).existsByCodigoInterno(any());
        verify(laboratorios, never()).findForUpdateByIdLaboratorio(999);
    }

    @Test
    void duplicadoDeSerieEnPutSeValidaAntesDeModificarEntidad() {
        actorEscritura("ADMIN");
        EquipoEntity entity = entity();
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(entity));
        when(subcategorias.findForUpdateByIdSubcategoriaAndActivoTrue(2)).thenReturn(Optional.of(subcategoria(2)));
        when(laboratorios.findForUpdateByIdLaboratorio(3)).thenReturn(Optional.of(entity.getLaboratorio()));
        when(equipos.existsBySerieUtecAndIdEquipoNot("REPETIDA", 7)).thenReturn(true);
        Equipo change = input();
        change.setSerieUtec("  REPETIDA  ");
        change.setNombre("No guardar");
        assertThrows(ConflictException.class, () -> service.actualizarEquipo(1, 7, change));
        assertEquals("Original", entity.getNombre());
        assertEquals(ORIGINAL, entity.getFechaActualizacion());
        verify(equipos, never()).saveAndFlush(any());
    }

    @Test
    void bajaNoBorraEquipoYBloqueaSegundoDeleteOPutSinReactivarlo() {
        actorEscritura("ADMIN");
        EquipoEntity entity = entity();
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(entity));
        service.eliminarEquipo(1, 7);
        assertEquals(EstadoEquipo.BAJA, entity.getEstado());
        assertEquals(ORIGINAL, entity.getFechaCreacion());
        assertTrue(entity.getFechaActualizacion().isAfter(ORIGINAL));
        assertThrows(ConflictException.class, () -> service.eliminarEquipo(1, 7));
        assertThrows(ConflictException.class, () -> service.actualizarEquipo(1, 7, input()));
        verify(equipos).saveAndFlush(entity);
        verify(equipos, never()).delete(any());
        verifyNoInteractions(subcategorias, laboratorios);
    }

    private Usuario actor(String role) { return Usuario.builder().id(1).userName("actor").rol(role).activo(true).build(); }
    private void actorEscritura(String role) {
        when(usuarios.findIdForShareByIdUsuario(1)).thenReturn(Optional.of(1));
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor(role)));
    }
    private SubcategoriaEntity subcategoria(int id) { return SubcategoriaEntity.builder().idSubcategoria(id).nombre("Subcategoría").activo(true).build(); }
    private LaboratorioEntity laboratorio(int id) { return LaboratorioEntity.builder().idLaboratorio(id).codigo("L" + id).nombre("Laboratorio").activo(true).build(); }
    private Equipo input() {
        return Equipo.builder().codigoInterno("EQ-1").nombre("Equipo").estado(EstadoEquipo.OPERATIVO)
                .subcategoria(Subcategoria.builder().id(2).build()).laboratorio(Laboratorio.builder().id(3).build()).build();
    }
    private EquipoEntity entity() {
        return EquipoEntity.builder().idEquipo(7).codigoInterno("EQ-1").nombre("Original").marca("Antes")
                .estado(EstadoEquipo.OPERATIVO).subcategoria(subcategoria(2)).laboratorio(laboratorio(3))
                .fechaCreacion(ORIGINAL).fechaActualizacion(ORIGINAL).build();
    }
}
