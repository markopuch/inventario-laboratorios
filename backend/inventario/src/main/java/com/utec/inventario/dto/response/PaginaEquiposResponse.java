package com.utec.inventario.dto.response;

import java.util.List;

public record PaginaEquiposResponse(List<EquipoResponse> content, int number, int size,
        long totalElements, int totalPages, boolean first, boolean last) {
}
