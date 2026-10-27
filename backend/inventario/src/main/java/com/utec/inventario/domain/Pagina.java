package com.utec.inventario.domain;

import java.util.List;

/** Resultado de una consulta limitada, independiente de JPA y del transporte HTTP. */
public record Pagina<T>(List<T> content, int number, int size, long totalElements,
        int totalPages, boolean first, boolean last) {
    public Pagina {
        content = List.copyOf(content);
    }
}
