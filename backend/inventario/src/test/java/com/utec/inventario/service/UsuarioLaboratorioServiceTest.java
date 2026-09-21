package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioId;
import com.utec.inventario.exception.ConflictException;
import com.utec.inventario.exception.ResourceNotFoundException;
import com.utec.inventario.mapper.UsuarioLaboratorioMapper;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.UsuarioLaboratorioRepository;
import com.utec.inventario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioLaboratorioServiceTest {
    @Mock private UsuarioRepository usuarios;
    @Mock private LaboratorioRepository laboratorios;
    @Mock private UsuarioLaboratorioRepository asignaciones;
    private UsuarioLaboratorioService service;

    @BeforeEach
    void preparar() {
        service = new UsuarioLaboratorioService(usuarios, laboratorios, asignaciones,
                Mappers.getMapper(UsuarioLaboratorioMapper.class));
    }

    @Test
    void consultaUsuarioPublicoSinAsignacionesYRechazaUsuarioInexistente() {
        when(usuarios.findPublicByIdUsuario(2)).thenReturn(Optional.of(usuario(true)), Optional.empty());
        assertTrue(service.listarAsignacionesActivas(2).getLaboratorios().isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.listarAsignacionesActivas(2));
        assertThrows(ResourceNotFoundException.class, () -> service.reemplazarAsignaciones(999, List.of(7)));
        verify(asignaciones).findAllByUsuario_IdUsuarioAndActivoTrueOrderByLaboratorio_IdLaboratorioAsc(2);
        verifyNoMoreInteractions(asignaciones);
        verifyNoInteractions(laboratorios);
        verify(usuarios, never()).findById(any());
        verify(usuarios, never()).findByUserNameIgnoreCase(any());
    }

    @Test
    void bloqueaPrimeroUsuarioYDestinosUnicosEnOrdenAntesDeCrearRelaciones() {
        configurarUsuario(true);
        LaboratorioEntity seven = laboratorio(7);
        LaboratorioEntity nine = laboratorio(9);
        UsuarioEntity reference = mock(UsuarioEntity.class);
        when(usuarios.getReferenceById(2)).thenReturn(reference);
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(7)).thenReturn(Optional.of(seven));
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9)).thenReturn(Optional.of(nine));
        when(asignaciones.findAllByUsuario_IdUsuarioAndActivoTrueOrderByLaboratorio_IdLaboratorioAsc(2))
                .thenReturn(List.of(asignacion(7, true), asignacion(9, true)));

        var result = service.reemplazarAsignaciones(2, List.of(9, 7, 9, 7));

        var ordered = inOrder(usuarios, laboratorios, asignaciones);
        ordered.verify(usuarios).findIdForUpdateByIdUsuario(2);
        ordered.verify(usuarios).findPublicByIdUsuario(2);
        ordered.verify(laboratorios).findForUpdateByIdLaboratorioAndActivoTrue(7);
        ordered.verify(laboratorios).findForUpdateByIdLaboratorioAndActivoTrue(9);
        ordered.verify(asignaciones).findAllByUsuario_IdUsuarioOrderByLaboratorio_IdLaboratorioAsc(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UsuarioLaboratorioEntity>> capture = ArgumentCaptor.forClass(List.class);
        verify(asignaciones).saveAllAndFlush(capture.capture());
        List<UsuarioLaboratorioEntity> inserted = capture.getValue();
        assertEquals(List.of(7, 9), inserted.stream().map(row -> row.getId().getIdLaboratorio()).toList());
        for (UsuarioLaboratorioEntity row : inserted) {
            assertEquals(2, row.getId().getIdUsuario());
            assertSame(reference, row.getUsuario());
            assertTrue(row.isActivo());
            assertNull(row.getFechaAsignacion(), "PostgreSQL genera la fecha original");
        }
        assertSame(seven, inserted.get(0).getLaboratorio());
        assertSame(nine, inserted.get(1).getLaboratorio());
        assertEquals(List.of(7, 9), result.getLaboratorios().stream().map(lab -> lab.getId()).toList());
        verify(usuarios).getReferenceById(2);
        verifyNoInteractions(reference);
    }

    @Test
    void reemplazoConservaFilasYFechasReactivandoSinReinsertarYNoEscribeSiEsIdempotente() {
        configurarUsuario(true);
        UsuarioLaboratorioEntity old = asignacion(5, true);
        UsuarioLaboratorioEntity maintained = asignacion(7, true);
        UsuarioLaboratorioEntity reactivated = asignacion(9, false);
        UsuarioLaboratorioEntity untouched = asignacion(11, false);
        List<UsuarioLaboratorioEntity> rows = new ArrayList<>(List.of(old, maintained, reactivated, untouched));
        OffsetDateTime original = reactivated.getFechaAsignacion();
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(7)).thenReturn(Optional.of(maintained.getLaboratorio()));
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9)).thenReturn(Optional.of(reactivated.getLaboratorio()));
        when(asignaciones.findAllByUsuario_IdUsuarioOrderByLaboratorio_IdLaboratorioAsc(2)).thenReturn(rows);
        when(asignaciones.findAllByUsuario_IdUsuarioAndActivoTrueOrderByLaboratorio_IdLaboratorioAsc(2))
                .thenAnswer(call -> rows.stream().filter(UsuarioLaboratorioEntity::isActivo).toList());
        assertEquals(2, service.reemplazarAsignaciones(2, List.of(9, 7)).getLaboratorios().size());
        assertFalse(old.isActivo());
        assertTrue(maintained.isActivo());
        assertTrue(reactivated.isActivo());
        assertFalse(untouched.isActivo());
        assertEquals(original, reactivated.getFechaAsignacion());
        verify(asignaciones).saveAllAndFlush(List.of(old, reactivated));
        assertEquals(2, service.reemplazarAsignaciones(2, List.of(7, 9, 9)).getLaboratorios().size());
        verify(asignaciones, times(1)).saveAllAndFlush(anyList());
        assertTrue(service.reemplazarAsignaciones(2, List.of()).getLaboratorios().isEmpty());
        verify(asignaciones).saveAllAndFlush(List.of(maintained, reactivated));
        assertEquals(original, reactivated.getFechaAsignacion());
        assertEquals(4, rows.size());
        verify(usuarios, never()).getReferenceById(any());
        verify(asignaciones, never()).delete(any());
    }

    @Test
    void validarTodosLosDestinosPrecedeCualquierLecturaOEscrituraDeAsignaciones() {
        configurarUsuario(true);
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(7)).thenReturn(Optional.of(laboratorio(7)));
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(9)).thenReturn(Optional.empty());
        when(laboratorios.existsById(9)).thenReturn(false, true);
        assertThrows(ResourceNotFoundException.class, () -> service.reemplazarAsignaciones(2, List.of(7, 9)));
        assertThrows(ConflictException.class, () -> service.reemplazarAsignaciones(2, List.of(7, 9)));
        verifyNoInteractions(asignaciones);
        verify(usuarios, never()).getReferenceById(any());
    }

    @Test
    void usuarioInactivoAdmitePrepararConfiguracionSinUsarConsultaDeAutenticacion() {
        configurarUsuario(false);
        UsuarioLaboratorioEntity row = asignacion(7, false);
        OffsetDateTime original = row.getFechaAsignacion();
        when(laboratorios.findForUpdateByIdLaboratorioAndActivoTrue(7)).thenReturn(Optional.of(row.getLaboratorio()));
        when(asignaciones.findAllByUsuario_IdUsuarioOrderByLaboratorio_IdLaboratorioAsc(2)).thenReturn(List.of(row));
        when(asignaciones.findAllByUsuario_IdUsuarioAndActivoTrueOrderByLaboratorio_IdLaboratorioAsc(2)).thenReturn(List.of(row));
        var result = service.reemplazarAsignaciones(2, List.of(7));
        assertFalse(result.getUsuario().isActivo());
        assertTrue(row.isActivo());
        assertEquals(original, row.getFechaAsignacion());
        verify(usuarios).findIdForUpdateByIdUsuario(2);
        verify(usuarios).findPublicByIdUsuario(2);
        verifyNoMoreInteractions(usuarios);
    }

    private void configurarUsuario(boolean active) {
        when(usuarios.findIdForUpdateByIdUsuario(2)).thenReturn(Optional.of(2));
        when(usuarios.findPublicByIdUsuario(2)).thenReturn(Optional.of(usuario(active)));
    }

    private Usuario usuario(boolean active) {
        return Usuario.builder().id(2).userName("gestor").rol("GESTOR").activo(active).build();
    }

    private LaboratorioEntity laboratorio(int id) {
        return LaboratorioEntity.builder().idLaboratorio(id).codigo("L" + id).nombre("Laboratorio " + id).activo(true).build();
    }

    private UsuarioLaboratorioEntity asignacion(int lab, boolean active) {
        return UsuarioLaboratorioEntity.builder().id(new UsuarioLaboratorioId(2, lab))
                .laboratorio(laboratorio(lab)).activo(active)
                .fechaAsignacion(OffsetDateTime.parse("2026-01-10T15:00:00Z")).build();
    }
}
