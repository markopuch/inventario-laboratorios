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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.MovimientoEquipoEntity;
import com.utec.inventario.entity.SubcategoriaEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.mapper.MovimientoEquipoMapper;
import com.utec.inventario.repository.EquipoRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.MovimientoEquipoRepository;
import com.utec.inventario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MovimientoEquipoServiceTest {
    private static final OffsetDateTime ORIGINAL = OffsetDateTime.parse("2026-01-10T15:00:00Z");
    @Mock private MovimientoEquipoRepository movimientos;
    @Mock private EquipoRepository equipos;
    @Mock private LaboratorioRepository laboratorios;
    @Mock private UsuarioRepository usuarios;
    @Mock private AlcanceLaboratorioService alcance;
    private MovimientoEquipoService service;

    @BeforeEach
    void preparar() {
        service = new MovimientoEquipoService(movimientos, equipos, laboratorios, usuarios, alcance,
                Mappers.getMapper(MovimientoEquipoMapper.class), Mappers.getMapper(EquipoMapper.class));
    }

    @Test
    void trasladoBloqueaActorEquipoYLaboratoriosOrdenadosYConservaSnapshotDelOrigen() {
        actorEscritura("ADMIN");
        EquipoEntity equipo = equipo();
        LaboratorioEntity origin = equipo.getLaboratorio();
        LaboratorioEntity destination = laboratorio(3);
        UsuarioEntity responsable = UsuarioEntity.builder().idUsuario(4).activo(false).build();
        equipo.setResponsable(responsable);
        UsuarioEntity actorRef = mock(UsuarioEntity.class);
        when(usuarios.getReferenceById(1)).thenReturn(actorRef);
        when(usuarios.findPublicByIdUsuario(4)).thenReturn(Optional.of(Usuario.builder().id(4).userName("responsable").activo(false).build()));
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo));
        when(laboratorios.findForUpdateByIdLaboratorio(3)).thenReturn(Optional.of(destination));
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(origin));
        when(equipos.saveAndFlush(equipo)).thenReturn(equipo);
        when(movimientos.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        var result = service.trasladarEquipo(1, 7, 3, "  Curso  ", "  Armario B  ");

        var order = inOrder(usuarios, equipos, laboratorios, movimientos);
        order.verify(usuarios).findIdForShareByIdUsuario(1);
        order.verify(usuarios).findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1);
        order.verify(equipos).findForUpdateByIdEquipo(7);
        order.verify(laboratorios).findForUpdateByIdLaboratorio(3);
        order.verify(laboratorios).findForUpdateByIdLaboratorio(9);
        order.verify(usuarios).getReferenceById(1);
        order.verify(equipos).saveAndFlush(equipo);
        ArgumentCaptor<MovimientoEquipoEntity> capture = ArgumentCaptor.forClass(MovimientoEquipoEntity.class);
        order.verify(movimientos).saveAndFlush(capture.capture());
        MovimientoEquipoEntity event = capture.getValue();
        assertSame(origin, event.getLaboratorioOrigen());
        assertSame(destination, event.getLaboratorioDestino());
        assertSame(destination, equipo.getLaboratorio());
        assertSame(equipo, event.getEquipo());
        assertSame(actorRef, event.getUsuarioActor());
        assertNull(event.getIdMovimiento());
        assertNull(event.getFechaMovimiento(), "PostgreSQL genera la fecha");
        assertEquals("TRASLADO", event.getTipoMovimiento());
        assertEquals("Curso", event.getMotivo());
        assertEquals("Armario B", equipo.getUbicacionInterna());
        assertSame(responsable, equipo.getResponsable());
        assertEquals(2, equipo.getSubcategoria().getIdSubcategoria());
        assertEquals("EQ-7", equipo.getCodigoInterno());
        assertEquals(ORIGINAL, equipo.getFechaCreacion());
        assertTrue(equipo.getFechaActualizacion().isAfter(ORIGINAL));
        assertEquals(9, result.getMovimiento().getLaboratorioOrigen().getId());
        assertEquals(3, result.getEquipo().getLaboratorio().getId());
        verifyNoInteractions(alcance, actorRef);
    }

    @Test
    void destinoAusenteOInactivoSeValidaAntesDeModificarOInsertar() {
        actorEscritura("ADMIN");
        EquipoEntity equipo = equipo();
        LaboratorioEntity origin = equipo.getLaboratorio();
        LaboratorioEntity inactive = laboratorio(3); inactive.setActivo(false);
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo));
        when(laboratorios.findForUpdateByIdLaboratorio(3)).thenReturn(Optional.empty(), Optional.of(inactive));
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(origin));
        assertThrows(ResourceNotFoundException.class, () -> service.trasladarEquipo(1, 7, 3, "Motivo", null));
        assertThrows(ConflictException.class, () -> service.trasladarEquipo(1, 7, 3, "Motivo", null));
        assertSame(origin, equipo.getLaboratorio());
        assertEquals(ORIGINAL, equipo.getFechaActualizacion());
        assertEquals("Armario anterior", equipo.getUbicacionInterna());
        verify(equipos, never()).saveAndFlush(any());
        verifyNoInteractions(movimientos, alcance);
    }

    @Test
    void lectorBajaYMismoDestinoSeRechazanSinLlegarAPersistencia() {
        actorEscritura("LECTOR");
        assertThrows(AccessDeniedException.class, () -> service.trasladarEquipo(1, 7, 3, "Motivo", null));
        verifyNoInteractions(equipos);
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("ADMIN")));
        EquipoEntity equipo = equipo();
        equipo.setEstado(EstadoEquipo.BAJA);
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo));
        assertThrows(ConflictException.class, () -> service.trasladarEquipo(1, 7, 3, "Motivo", null));
        equipo.setEstado(EstadoEquipo.OPERATIVO);
        assertThrows(ConflictException.class, () -> service.trasladarEquipo(1, 7, 9, "Motivo", null));
        verifyNoInteractions(laboratorios, movimientos, alcance);
        verify(equipos, never()).saveAndFlush(any());
    }

    @Test
    void gestorDebeSuperarAmbasComprobacionesDeAlcanceAntesDeModificarEquipo() {
        actorEscritura("GESTOR");
        EquipoEntity equipo = equipo();
        LaboratorioEntity origin = equipo.getLaboratorio();
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo));
        when(laboratorios.findForUpdateByIdLaboratorio(3)).thenReturn(Optional.of(laboratorio(3)));
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(origin));
        doNothing().when(alcance).validarAccesoLaboratorio(1,9);
        doThrow(new AccessDeniedException("Sin destino")).when(alcance).validarAccesoLaboratorio(1,3);
        assertThrows(AccessDeniedException.class, () -> service.trasladarEquipo(1, 7, 3, "Motivo", null));
        verify(alcance).validarAccesoLaboratorio(1,9);
        verify(alcance).validarAccesoLaboratorio(1,3);
        assertSame(origin, equipo.getLaboratorio());
        assertEquals(ORIGINAL, equipo.getFechaActualizacion());
        verify(equipos, never()).saveAndFlush(any());
        verifyNoInteractions(movimientos);
    }

    @Test
    void historiaVisibleFueraDelLaboratorioActualSeConsultaEnSqlYResuelveActoresPorLote() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("GESTOR")));
        EquipoEntity equipo = equipo();
        UsuarioEntity actorJpa = mock(UsuarioEntity.class);
        when(actorJpa.getIdUsuario()).thenReturn(4);
        MovimientoEquipoEntity event = MovimientoEquipoEntity.builder().idMovimiento(10).equipo(equipo)
                .laboratorioOrigen(laboratorio(3)).laboratorioDestino(laboratorio(9)).usuarioActor(actorJpa)
                .tipoMovimiento("TRASLADO").fechaMovimiento(ORIGINAL).build();
        when(equipos.findByIdEquipo(7)).thenReturn(Optional.of(equipo));
        when(alcance.obtenerAlcanceEfectivo(1)).thenReturn(scope(3));
        when(movimientos.findAllVisibles(7, null, List.of(3))).thenReturn(List.of(event));
        when(movimientos.findAllVisibles(null, 3, List.of(3))).thenReturn(List.of(event));
        when(usuarios.findPublicByIdUsuarioIn(List.of(4))).thenReturn(List.of(Usuario.builder().id(4).userName("actor histórico").build()));
        assertEquals(10, service.listarPorEquipo(1,7).getFirst().getId());
        assertEquals(10, service.listarMovimientos(1,3).getFirst().getId());
        verify(movimientos).findAllVisibles(7, null, List.of(3));
        verify(movimientos).findAllVisibles(null, 3, List.of(3));
        verify(alcance).validarAccesoLaboratorio(1,3);
        verify(usuarios, times(2)).findPublicByIdUsuarioIn(List.of(4));
        verify(actorJpa, never()).getPasswordHash();
        verify(movimientos, never()).findAll();
        verifyNoMoreInteractions(movimientos);
    }

    @Test
    void ausenciaDeHistoriaDevuelveVacioSoloConEquipoActualVisibleYAlcanceVacioNoConsultaTabla() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("LECTOR")));
        when(equipos.findByIdEquipo(7)).thenReturn(Optional.of(equipo()));
        when(alcance.obtenerAlcanceEfectivo(1)).thenReturn(scope(9), scope(3), scope(), scope());
        assertTrue(service.listarPorEquipo(1,7).isEmpty());
        assertThrows(AccessDeniedException.class, () -> service.listarPorEquipo(1,7));
        assertTrue(service.listarMovimientos(1,null).isEmpty());
        assertThrows(AccessDeniedException.class, () -> service.listarPorEquipo(1,7));
        verify(movimientos).findAllVisibles(7,null,List.of(9));
        verify(movimientos).findAllVisibles(7,null,List.of(3));
        verifyNoMoreInteractions(movimientos);
    }

    private Usuario actor(String role) { return Usuario.builder().id(1).userName("actor").rol(role).activo(true).build(); }
    private void actorEscritura(String role) {
        when(usuarios.findIdForShareByIdUsuario(1)).thenReturn(Optional.of(1));
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor(role)));
    }
    private LaboratorioEntity laboratorio(int id) { return LaboratorioEntity.builder().idLaboratorio(id).codigo("L" + id).nombre("Laboratorio " + id).activo(true).build(); }
    private EquipoEntity equipo() {
        return EquipoEntity.builder().idEquipo(7).codigoInterno("EQ-7").nombre("Equipo").estado(EstadoEquipo.OPERATIVO)
                .subcategoria(SubcategoriaEntity.builder().idSubcategoria(2).build()).laboratorio(laboratorio(9))
                .ubicacionInterna("Armario anterior").fechaCreacion(ORIGINAL).fechaActualizacion(ORIGINAL).build();
    }
    private AlcanceLaboratorios scope(Integer... ids) {
        return AlcanceLaboratorios.builder().alcanceGlobal(false)
                .laboratorios(List.of(ids).stream().map(id -> Laboratorio.builder().id(id).build()).toList()).build();
    }
}
