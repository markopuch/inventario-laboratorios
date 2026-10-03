package com.utec.inventario.service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.utec.inventario.domain.*;
import com.utec.inventario.entity.AuditoriaEntity;
import com.utec.inventario.mapper.AuditoriaMapper;
import com.utec.inventario.repository.*;
import com.utec.inventario.security.UserInfoDetails;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class AuditoriaService {
    private final AuditoriaRepository repository;
    private final UsuarioRepository usuarios;
    private final AuditoriaMapper mapper;

    @Autowired
    public AuditoriaService(AuditoriaRepository repository, UsuarioRepository usuarios, AuditoriaMapper mapper) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.mapper = mapper;
    }

    // Los callers aportan únicamente campos seleccionados, nunca cuerpos HTTP ni secretos.
    // Se guarda dentro de la misma transacción que el cambio; un rollback no deja evento.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(AccionAuditoria accion, String entidad, Integer idEntidad, String cambios) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserInfoDetails actor)) {
            // Arranque/fixtures internos no son acciones de una API autenticada.
            return;
        }
        this.repository.saveAndFlush(AuditoriaEntity.builder()
                .usuarioActor(this.usuarios.getReferenceById(actor.getId()))
                .userNameActor(actor.getUsername())
                .accion(accion).entidad(entidad).idEntidad(idEntidad).cambios(cambios).build());
    }

    public List<Auditoria> listar(FiltroAuditoria filtro) {
        Specification<AuditoriaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filtro.getEntidad() != null) predicates.add(cb.equal(root.get("entidad"), filtro.getEntidad()));
            if (filtro.getIdEntidad() != null) predicates.add(cb.equal(root.get("idEntidad"), filtro.getIdEntidad()));
            if (filtro.getIdUsuario() != null) predicates.add(cb.equal(root.get("usuarioActor").get("idUsuario"), filtro.getIdUsuario()));
            if (filtro.getAccion() != null) predicates.add(cb.equal(root.get("accion"), filtro.getAccion()));
            if (filtro.getFechaDesde() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"),
                    filtro.getFechaDesde().atStartOfDay().atOffset(ZoneOffset.UTC)));
            if (filtro.getFechaHasta() != null) predicates.add(cb.lessThan(root.get("fecha"),
                    filtro.getFechaHasta().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return this.mapper.convert(this.repository.findAll(spec,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("idAuditoria"))));
    }
}

