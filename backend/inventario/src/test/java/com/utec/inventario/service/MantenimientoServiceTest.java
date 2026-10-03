package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
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

import com.utec.inventario.domain.AccionAuditoria;
import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.domain.EstadoMantenimiento;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Mantenimiento;
import com.utec.inventario.domain.TipoMantenimiento;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.MantenimientoEntity;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.MantenimientoMapper;
import com.utec.inventario.repository.EquipoRepository;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.MantenimientoRepository;
import com.utec.inventario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceTest {
    @Mock private MantenimientoRepository mantenimientos;
    @Mock private EquipoRepository equipos;
    @Mock private LaboratorioRepository laboratorios;
    @Mock private UsuarioRepository usuarios;
    @Mock private AlcanceLaboratorioService alcance;
    @Mock private AuditoriaService auditoria;
    private MantenimientoService service;

    @BeforeEach
    void preparar() {
        service = new MantenimientoService(mantenimientos, equipos, laboratorios, usuarios, alcance,
                Mappers.getMapper(MantenimientoMapper.class), auditoria);
    }

    @Test
    void programarNoCambiaEstadoDelEquipoYDescartaDatosGeneradosDelDomain() {
        actorEscritura("ADMIN");
        EquipoEntity equipo = equipo();
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo));
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(equipo.getLaboratorio()));
        when(mantenimientos.saveAndFlush(any())).thenAnswer(call -> { var entity = (MantenimientoEntity) call.getArgument(0); entity.setIdMantenimiento(12); return entity; });
        Mantenimiento datos = datos();
        datos.setEstado(EstadoMantenimiento.COMPLETADO);
        datos.setFechaInicio(OffsetDateTime.now());
        datos.setFechaFin(OffsetDateTime.now());
        datos.setEstadoEquipoAnterior(EstadoEquipo.BAJA);
        datos.setId(200);

        Mantenimiento response = service.crearMantenimiento(1, datos);

        ArgumentCaptor<MantenimientoEntity> captor = ArgumentCaptor.forClass(MantenimientoEntity.class);
        verify(mantenimientos).saveAndFlush(captor.capture());
        var guardado = captor.getValue();
        assertEquals(EstadoMantenimiento.PROGRAMADO, guardado.getEstado());
        assertEquals("Revisión", guardado.getDescripcion());
        assertNull(guardado.getFechaInicio());
        assertNull(guardado.getFechaFin());
        assertNull(guardado.getEstadoEquipoAnterior());
        assertNull(guardado.getFechaCreacion());
        assertEquals(EstadoEquipo.OPERATIVO, equipo.getEstado());
        assertEquals(12, response.getId());
        verify(equipos, never()).saveAndFlush(any());
        verify(auditoria).registrar(eq(AccionAuditoria.CREAR), eq("mantenimiento"), eq(12), anyString());
    }

    @Test
    void lectorNoEscribeAunqueLlameDirectamenteAlService() {
        actorEscritura("LECTOR");
        assertThrows(AccessDeniedException.class, () -> service.crearMantenimiento(1, datos()));
        verifyNoInteractions(equipos, mantenimientos, auditoria);
    }

    @Test
    void equipoBajaNoSePuedeProgramar() {
        actorEscritura("ADMIN");
        EquipoEntity equipo = equipo(); equipo.setEstado(EstadoEquipo.BAJA);
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo));
        assertThrows(ConflictException.class, () -> service.crearMantenimiento(1, datos()));
        verifyNoInteractions(laboratorios, mantenimientos, auditoria);
    }

    @Test
    void gestorFueraDeAlcanceNoPrograma() {
        actorEscritura("GESTOR");
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo()));
        doThrow(new AccessDeniedException("Sin alcance")).when(alcance).validarAccesoLaboratorio(1, 9);
        assertThrows(AccessDeniedException.class, () -> service.crearMantenimiento(1, datos()));
        verifyNoInteractions(laboratorios, mantenimientos, auditoria);
    }

    @Test
    void nuevoResponsableInactivoProduceConflicto() {
        actorEscritura("ADMIN");
        EquipoEntity equipo = equipo();
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(equipo));
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(equipo.getLaboratorio()));
        when(usuarios.findIdForShareByIdUsuario(3)).thenReturn(Optional.of(3));
        when(usuarios.findPublicByIdUsuario(3)).thenReturn(Optional.of(Usuario.builder().id(3).activo(false).build()));
        Mantenimiento datos = datos(); datos.setResponsable(Usuario.builder().id(3).build());
        assertThrows(ConflictException.class, () -> service.crearMantenimiento(1, datos));
        verify(mantenimientos, never()).saveAndFlush(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void iniciarYCompletarRestauraInoperativoYFechasDelServidor() {
        actorEscritura("ADMIN");
        MantenimientoEntity mantenimiento = programado();
        mantenimiento.getEquipo().setEstado(EstadoEquipo.INOPERATIVO);
        bloquear(mantenimiento);
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(mantenimiento.getEquipo().getLaboratorio()));
        when(mantenimientos.existsByEquipo_IdEquipoAndEstado(7, EstadoMantenimiento.EN_PROCESO)).thenReturn(false);
        when(mantenimientos.saveAndFlush(mantenimiento)).thenReturn(mantenimiento);
        when(equipos.saveAndFlush(mantenimiento.getEquipo())).thenReturn(mantenimiento.getEquipo());

        Mantenimiento iniciado = service.cambiarEstado(1, 12, EstadoMantenimiento.EN_PROCESO, null);
        assertEquals(EstadoMantenimiento.EN_PROCESO, iniciado.getEstado());
        assertEquals(EstadoEquipo.MANTENIMIENTO, mantenimiento.getEquipo().getEstado());
        assertEquals(EstadoEquipo.INOPERATIVO, mantenimiento.getEstadoEquipoAnterior());
        assertNotNull(iniciado.getFechaInicio());
        assertNull(iniciado.getFechaFin());

        Mantenimiento completo = service.cambiarEstado(1, 12, EstadoMantenimiento.COMPLETADO, "  Finalizado  ");
        assertEquals(EstadoEquipo.INOPERATIVO, mantenimiento.getEquipo().getEstado());
        assertEquals("Finalizado", completo.getObservaciones());
        assertNotNull(completo.getFechaFin());
        assertFalse(completo.getFechaFin().isBefore(completo.getFechaInicio()));
        verify(auditoria).registrar(eq(AccionAuditoria.INICIAR_MANTENIMIENTO), eq("mantenimiento"), eq(12), anyString());
        verify(auditoria).registrar(eq(AccionAuditoria.COMPLETAR_MANTENIMIENTO), eq("mantenimiento"), eq(12), anyString());
    }

    @Test
    void cancelarEnProcesoRestauraOperativo() {
        actorEscritura("ADMIN");
        MantenimientoEntity mantenimiento = programado();
        mantenimiento.setEstado(EstadoMantenimiento.EN_PROCESO);
        mantenimiento.setFechaInicio(OffsetDateTime.now().minusDays(1));
        mantenimiento.setEstadoEquipoAnterior(EstadoEquipo.OPERATIVO);
        mantenimiento.getEquipo().setEstado(EstadoEquipo.MANTENIMIENTO);
        bloquear(mantenimiento);
        when(mantenimientos.saveAndFlush(mantenimiento)).thenReturn(mantenimiento);
        when(equipos.saveAndFlush(mantenimiento.getEquipo())).thenReturn(mantenimiento.getEquipo());
        assertEquals(EstadoMantenimiento.CANCELADO, service.cambiarEstado(1, 12, EstadoMantenimiento.CANCELADO, null).getEstado());
        assertEquals(EstadoEquipo.OPERATIVO, mantenimiento.getEquipo().getEstado());
    }

    @Test
    void cancelarProgramadoPermiteCerrarAgendaTrasBajaSinModificarEquipo() {
        actorEscritura("ADMIN");
        MantenimientoEntity mantenimiento = programado();
        mantenimiento.getEquipo().setEstado(EstadoEquipo.BAJA);
        bloquear(mantenimiento);
        when(mantenimientos.saveAndFlush(mantenimiento)).thenReturn(mantenimiento);
        var result = service.cambiarEstado(1, 12, EstadoMantenimiento.CANCELADO, null);
        assertNull(result.getFechaInicio());
        assertNotNull(result.getFechaFin());
        assertNull(mantenimiento.getEstadoEquipoAnterior());
        assertEquals(EstadoEquipo.BAJA, mantenimiento.getEquipo().getEstado());
        verify(equipos, never()).saveAndFlush(any());
    }

    @Test
    void noIniciaSegundoMantenimientoActivoYBloqueaEquipoAntesDelRegistro() {
        actorEscritura("ADMIN");
        MantenimientoEntity mantenimiento = programado(); bloquear(mantenimiento);
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(mantenimiento.getEquipo().getLaboratorio()));
        when(mantenimientos.existsByEquipo_IdEquipoAndEstado(7, EstadoMantenimiento.EN_PROCESO)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.cambiarEstado(1, 12, EstadoMantenimiento.EN_PROCESO, null));
        var orden = inOrder(usuarios, mantenimientos, equipos);
        orden.verify(usuarios).findIdForShareByIdUsuario(1);
        orden.verify(usuarios).findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1);
        orden.verify(mantenimientos).findIdEquipoByIdMantenimiento(12);
        orden.verify(equipos).findForUpdateByIdEquipo(7);
        orden.verify(mantenimientos).findForUpdateByIdMantenimiento(12);
        assertEquals(EstadoEquipo.OPERATIVO, mantenimiento.getEquipo().getEstado());
        assertEquals(EstadoMantenimiento.PROGRAMADO, mantenimiento.getEstado());
        verify(mantenimientos, never()).saveAndFlush(any());
    }

    @Test
    void rechazaSaltosReinicioYEstadosFinales() {
        actorEscritura("ADMIN");
        MantenimientoEntity mantenimiento = programado(); bloquear(mantenimiento);
        for (EstadoMantenimiento estado : List.of(EstadoMantenimiento.PROGRAMADO, EstadoMantenimiento.COMPLETADO)) {
            assertThrows(ConflictException.class, () -> service.cambiarEstado(1, 12, estado, null));
        }
        for (EstadoMantenimiento finalizado : List.of(EstadoMantenimiento.COMPLETADO, EstadoMantenimiento.CANCELADO)) {
            mantenimiento.setEstado(finalizado);
            assertThrows(ConflictException.class, () -> service.cambiarEstado(1, 12, EstadoMantenimiento.EN_PROCESO, null));
            assertThrows(ConflictException.class, () -> service.actualizarMantenimiento(1, 12, datos()));
        }
        verify(mantenimientos, never()).saveAndFlush(any());
        verify(equipos, never()).saveAndFlush(any());
    }

    @Test
    void editarProgramadoConservaEquipoYCamposDeCiclo() {
        actorEscritura("ADMIN");
        MantenimientoEntity mantenimiento = programado(); bloquear(mantenimiento);
        when(laboratorios.findForUpdateByIdLaboratorio(9)).thenReturn(Optional.of(mantenimiento.getEquipo().getLaboratorio()));
        when(mantenimientos.saveAndFlush(mantenimiento)).thenReturn(mantenimiento);
        Mantenimiento cambios = datos(); cambios.setTipo(TipoMantenimiento.CALIBRACION);
        var result = service.actualizarMantenimiento(1, 12, cambios);
        assertEquals(7, result.getEquipo().getId());
        assertEquals(TipoMantenimiento.CALIBRACION, result.getTipo());
        assertNull(result.getFechaInicio()); assertNull(result.getFechaFin());
        assertEquals(EstadoMantenimiento.PROGRAMADO, result.getEstado());
        assertNotNull(result.getFechaActualizacion());
        verify(equipos, never()).saveAndFlush(any());
    }

    @Test
    void historialSeConsultaConAlcanceActualYProyeccionPublicaSinResponsable() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("LECTOR")));
        when(alcance.obtenerAlcanceEfectivo(1)).thenReturn(AlcanceLaboratorios.builder().laboratorios(List.of(Laboratorio.builder().id(9).build())).build());
        when(mantenimientos.findAllFiltrados(null, null, null, null, null, null, List.of(9))).thenReturn(List.of(programado()));
        var listado = service.listarMantenimientos(1, null, null, null, null, null, null);
        assertEquals(1, listado.size()); assertNull(listado.getFirst().getResponsable());
        verify(usuarios, never()).findPublicByIdUsuarioIn(any());
    }

    @Test
    void usuarioSinAsignacionesObtieneListaVacia() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("GESTOR")));
        when(alcance.obtenerAlcanceEfectivo(1)).thenReturn(AlcanceLaboratorios.builder().laboratorios(List.of()).build());
        assertTrue(service.listarMantenimientos(1, null, null, null, null, null, null).isEmpty());
        verifyNoInteractions(mantenimientos);
    }

    @Test
    void detalleNoExisteDa404YDetalleFueraDeAlcance403() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor("LECTOR")));
        when(mantenimientos.findByIdMantenimiento(12)).thenReturn(Optional.empty(), Optional.of(programado()));
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerMantenimiento(1, 12));
        doThrow(new AccessDeniedException("Fuera de alcance")).when(alcance).validarAccesoLaboratorio(1, 9);
        assertThrows(AccessDeniedException.class, () -> service.obtenerMantenimiento(1, 12));
    }

    private void actorEscritura(String rol) {
        when(usuarios.findIdForShareByIdUsuario(1)).thenReturn(Optional.of(1));
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1)).thenReturn(Optional.of(actor(rol)));
    }
    private Usuario actor(String rol) { return Usuario.builder().id(1).activo(true).rol(rol).build(); }
    private EquipoEntity equipo() {
        return EquipoEntity.builder().idEquipo(7).codigoInterno("EQ-7").nombre("Equipo")
                .estado(EstadoEquipo.OPERATIVO).laboratorio(LaboratorioEntity.builder().idLaboratorio(9).activo(true).nombre("Laboratorio").build()).build();
    }
    private Mantenimiento datos() {
        return Mantenimiento.builder().equipo(Equipo.builder().id(7).build()).tipo(TipoMantenimiento.PREVENTIVO)
                .descripcion("  Revisión  ").fechaProgramada(LocalDate.of(2026, 10, 10)).build();
    }
    private MantenimientoEntity programado() {
        return MantenimientoEntity.builder().idMantenimiento(12).equipo(equipo()).tipo(TipoMantenimiento.PREVENTIVO)
                .descripcion("Revisión").fechaProgramada(LocalDate.of(2026, 10, 10)).estado(EstadoMantenimiento.PROGRAMADO).build();
    }
    private void bloquear(MantenimientoEntity mantenimiento) {
        when(mantenimientos.findIdEquipoByIdMantenimiento(12)).thenReturn(Optional.of(7));
        when(equipos.findForUpdateByIdEquipo(7)).thenReturn(Optional.of(mantenimiento.getEquipo()));
        when(mantenimientos.findForUpdateByIdMantenimiento(12)).thenReturn(Optional.of(mantenimiento));
    }
}
