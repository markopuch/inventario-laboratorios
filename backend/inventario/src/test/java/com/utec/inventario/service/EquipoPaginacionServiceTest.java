package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import com.utec.inventario.domain.AlcanceLaboratorios;
import com.utec.inventario.domain.Equipo;
import com.utec.inventario.domain.Laboratorio;
import com.utec.inventario.domain.Pagina;
import com.utec.inventario.domain.Usuario;
import com.utec.inventario.entity.EquipoEntity;
import com.utec.inventario.entity.LaboratorioEntity;
import com.utec.inventario.entity.SubcategoriaEntity;
import com.utec.inventario.entity.UsuarioEntity;
import com.utec.inventario.mapper.EquipoMapper;
import com.utec.inventario.repository.*;

@ExtendWith(MockitoExtension.class)
class EquipoPaginacionServiceTest {
    @Mock private EquipoRepository equipos;
    @Mock private SubcategoriaRepository subcategorias;
    @Mock private LaboratorioRepository laboratorios;
    @Mock private UsuarioRepository usuarios;
    @Mock private AlcanceLaboratorioService alcance;
    @Mock private MantenimientoRepository mantenimientos;
    @Mock private AuditoriaService auditoria;
    private EquipoService service;

    @BeforeEach
    void preparar() {
        service = new EquipoService(equipos, subcategorias, laboratorios, usuarios, alcance,
                Mappers.getMapper(EquipoMapper.class), mantenimientos, auditoria);
    }

    private void actor(String rol) {
        when(usuarios.findPublicByIdUsuarioAndActivoTrueAndRolActivoTrue(1))
                .thenReturn(Optional.of(Usuario.builder().id(1).rol(rol).activo(true).build()));
    }

    @Test
    void paginaResuelveSoloLosResponsablesDelContenidoYConservaElConteoTotal() {
        actor("ADMIN");
        UsuarioEntity responsable = mock(UsuarioEntity.class);
        when(responsable.getIdUsuario()).thenReturn(4);
        EquipoEntity entity = EquipoEntity.builder().idEquipo(3).codigoInterno("PG-3").nombre("Equipo")
                .laboratorio(LaboratorioEntity.builder().idLaboratorio(7).nombre("Lab").build())
                .subcategoria(SubcategoriaEntity.builder().idSubcategoria(2).nombre("Sub").build())
                .responsable(responsable).build();
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Order.asc("idEquipo")));
        Page<EquipoEntity> page = new PageImpl<>(List.of(entity), pageable, 7);
        when(equipos.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(usuarios.findPublicByIdUsuarioIn(List.of(4)))
                .thenReturn(List.of(Usuario.builder().id(4).userName("responsable").build()));
        Pagina<Equipo> resultado = service.listarEquiposPaginados(1, null, null, null, null, null, 1, 2);
        assertEquals(1, resultado.content().size());
        assertEquals(4, resultado.content().getFirst().getResponsable().getId());
        assertEquals(7, resultado.totalElements());
        assertEquals(4, resultado.totalPages());
        assertEquals(1, resultado.number());
        assertFalse(resultado.first());
        assertFalse(resultado.last());
        verify(usuarios).findPublicByIdUsuarioIn(List.of(4));
        verify(responsable, never()).getPasswordHash();
        verify(equipos, never()).findAll();
        verifyNoInteractions(mantenimientos, auditoria);
    }

    @Test
    void sinAsignacionesDevuelvePaginaVaciaSinConsultarTodoElInventario() {
        actor("LECTOR");
        when(alcance.obtenerAlcanceEfectivo(1)).thenReturn(AlcanceLaboratorios.builder()
                .alcanceGlobal(false).laboratorios(List.of()).build());
        Pagina<Equipo> resultado = service.listarEquiposPaginados(1, null, null, null, null, "dato", 4, 2);
        assertTrue(resultado.content().isEmpty());
        assertEquals(0, resultado.totalElements());
        assertEquals(4, resultado.number());
        assertEquals(2, resultado.size());
        assertFalse(resultado.first());
        assertTrue(resultado.last());
        assertTrue(service.listarEquiposPaginados(1, null, null, null, null, null,
                Integer.MAX_VALUE, 1).last());
        verifyNoInteractions(equipos);
    }

    @Test
    void filtroExplicitoFueraDeAlcanceSeRechazaAntesDeContar() {
        actor("GESTOR");
        doThrow(new AccessDeniedException("Fuera de alcance")).when(alcance).validarAccesoLaboratorio(1, 8);
        assertThrows(AccessDeniedException.class,
                () -> service.listarEquiposPaginados(1, null, 8, null, null, null, 0, 20));
        verifyNoInteractions(equipos);
    }

    @Test
    void busquedaVaciaConservaElListadoAnteriorSinIntroducirPaginacion() {
        actor("ADMIN");
        when(equipos.findAllFiltrados(null, null, null, null)).thenReturn(List.of());
        assertTrue(service.listarEquipos(1, null, null, null, null, "   ").isEmpty());
        verify(equipos).findAllFiltrados(null, null, null, null);
        verify(equipos, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
