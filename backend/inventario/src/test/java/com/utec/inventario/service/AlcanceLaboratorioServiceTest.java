package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.UsuarioLaboratorioEntity;
import com.utec.inventario.mapper.LaboratorioMapper;
import com.utec.inventario.repository.LaboratorioRepository;
import com.utec.inventario.repository.UsuarioLaboratorioRepository;
import com.utec.inventario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AlcanceLaboratorioServiceTest {
    @Mock private UsuarioRepository usuarios;
    @Mock private LaboratorioRepository laboratorios;
    @Mock private UsuarioLaboratorioRepository asignaciones;
    private AlcanceLaboratorioService service;

    @BeforeEach
    void preparar() {
        service = new AlcanceLaboratorioService(usuarios, laboratorios, asignaciones, Mappers.getMapper(LaboratorioMapper.class));
    }

    @Test
    void adminConsultaTodosLosActivosYHelperRechazaLaboratorioAusenteOInactivo() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(2)).thenReturn(Optional.of(usuario("ADMIN")));
        when(laboratorios.findAllByActivoTrueOrderByIdLaboratorioAsc()).thenReturn(List.of(laboratorio(7), laboratorio(9)));
        when(laboratorios.existsByIdLaboratorioAndActivoTrue(7)).thenReturn(true);
        when(laboratorios.existsByIdLaboratorioAndActivoTrue(8)).thenReturn(false);
        var result = service.obtenerAlcanceEfectivo(2);
        assertTrue(result.isAlcanceGlobal());
        assertEquals(List.of(7, 9), result.getLaboratorios().stream().map(lab -> lab.getId()).toList());
        assertTrue(service.tieneAccesoLaboratorio(2, 7));
        assertDoesNotThrow(() -> service.validarAccesoLaboratorio(2, 7));
        assertFalse(service.tieneAccesoLaboratorio(2, 8));
        assertThrows(AccessDeniedException.class, () -> service.validarAccesoLaboratorio(2, 8));
        for (Integer invalid : new Integer[] { null, 0, -1 }) assertFalse(service.tieneAccesoLaboratorio(2, invalid));
        verifyNoInteractions(asignaciones);
    }

    @Test
    void gestorYLectorExigenAsignacionYLaboratorioActivosParaListarYParaHelper() {
        UsuarioLaboratorioEntity row = UsuarioLaboratorioEntity.builder().laboratorio(laboratorio(7)).activo(true).build();
        when(asignaciones.findAllByUsuario_IdUsuarioAndActivoTrueAndLaboratorio_ActivoTrueOrderByLaboratorio_IdLaboratorioAsc(2))
                .thenReturn(List.of(row));
        when(asignaciones.existsByUsuario_IdUsuarioAndLaboratorio_IdLaboratorioAndActivoTrueAndLaboratorio_ActivoTrue(2, 7))
                .thenReturn(true);
        for (String role : List.of("GESTOR", "LECTOR")) {
            when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(2)).thenReturn(Optional.of(usuario(role)));
            var result = service.obtenerAlcanceEfectivo(2);
            assertFalse(result.isAlcanceGlobal());
            assertEquals(7, result.getLaboratorios().getFirst().getId());
            assertTrue(service.tieneAccesoLaboratorio(2, 7));
            assertFalse(service.tieneAccesoLaboratorio(2, 8));
            assertThrows(AccessDeniedException.class, () -> service.validarAccesoLaboratorio(2, 8));
        }
        verifyNoInteractions(laboratorios);
    }

    @Test
    void usuarioORolSinVigenciaNoAccedeYAunqueExistaUnRolDesconocidoSeRechaza() {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(2)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.obtenerAlcanceEfectivo(2));
        assertThrows(AccessDeniedException.class, () -> service.tieneAccesoLaboratorio(2, 7));
        assertThrows(AccessDeniedException.class, () -> service.validarAccesoLaboratorio(2, 7));
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(2)).thenReturn(Optional.of(usuario("OTRO")));
        assertThrows(AccessDeniedException.class, () -> service.obtenerAlcanceEfectivo(2));
        assertThrows(AccessDeniedException.class, () -> service.tieneAccesoLaboratorio(2, 7));
        verifyNoInteractions(laboratorios, asignaciones);
        verify(usuarios, never()).findById(any());
        verify(usuarios, never()).findByUserNameIgnoreCase(any());
    }

    private Usuario usuario(String role) {
        return Usuario.builder().id(2).userName("usuario").rol(role).activo(true).build();
    }

    private LaboratorioEntity laboratorio(int id) {
        return LaboratorioEntity.builder().idLaboratorio(id).codigo("L" + id).nombre("Laboratorio " + id).activo(true).build();
    }
}
