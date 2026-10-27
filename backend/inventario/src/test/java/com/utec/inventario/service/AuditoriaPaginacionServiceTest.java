package com.utec.inventario.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.utec.inventario.domain.FiltroAuditoria;
import com.utec.inventario.entity.AuditoriaEntity;
import com.utec.inventario.mapper.AuditoriaMapper;
import com.utec.inventario.repository.AuditoriaRepository;
import com.utec.inventario.repository.UsuarioRepository;

class AuditoriaPaginacionServiceTest {
    @Test
    void conservaLaPaginaSolicitadaYLosTotalesAunqueElContenidoEsteFueraDeRango() {
        AuditoriaRepository repository = mock(AuditoriaRepository.class);
        AuditoriaService service = new AuditoriaService(repository, mock(UsuarioRepository.class),
                Mappers.getMapper(AuditoriaMapper.class));
        Pageable pageable = PageRequest.of(9, 2,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("idAuditoria")));
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<AuditoriaEntity>(List.of(), pageable, 5));
        var resultado = service.listarPaginado(new FiltroAuditoria(), 9, 2);
        assertTrue(resultado.content().isEmpty());
        assertEquals(5, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        assertEquals(9, resultado.number());
        assertFalse(resultado.first());
        assertTrue(resultado.last());
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }
}
