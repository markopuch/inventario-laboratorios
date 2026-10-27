package com.utec.inventario.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.utec.inventario.domain.EstadoEquipo;
import com.utec.inventario.entity.EquipoEntity;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/** Los mismos predicados alimentan la consulta limitada y su COUNT filtrado. */
public final class EquipoEspecificaciones {
    private EquipoEspecificaciones() {
    }

    public static Specification<EquipoEntity> filtrar(EstadoEquipo estado, Integer idLaboratorio,
            Integer idSubcategoria, Boolean requiereMantenimiento, List<Integer> alcance, String patron) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (alcance != null) {
                predicates.add(root.get("laboratorio").get("idLaboratorio").in(alcance));
                predicates.add(cb.isTrue(root.get("laboratorio").get("activo")));
            }
            if (estado != null) predicates.add(cb.equal(root.get("estado"), estado));
            if (idLaboratorio != null) predicates.add(cb.equal(root.get("laboratorio").get("idLaboratorio"), idLaboratorio));
            if (idSubcategoria != null) predicates.add(cb.equal(root.get("subcategoria").get("idSubcategoria"), idSubcategoria));
            if (requiereMantenimiento != null) predicates.add(cb.equal(root.get("requiereMantenimiento"), requiereMantenimiento));
            if (patron != null) {
                // Equivale a los campos presentes concatenados con espacios en Equipos.jsx.
                // Permite también buscar un fragmento que cruce dos campos consecutivos.
                Expression<String> texto = root.get("nombre");
                List<Expression<String>> campos = List.of(root.get("codigoInterno"), root.get("marca"),
                        root.get("modelo"), root.get("laboratorio").get("nombre"));
                for (Expression<String> campo : campos) {
                    Expression<String> valor = cb.coalesce(campo, "");
                    Expression<String> fragmento = cb.<String>selectCase()
                            .when(cb.equal(valor, ""), "")
                            .otherwise(cb.concat(" ", valor));
                    texto = cb.concat(texto, fragmento);
                }
                predicates.add(cb.like(cb.lower(texto), patron, '!'));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
