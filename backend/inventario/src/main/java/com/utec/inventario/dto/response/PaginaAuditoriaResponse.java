package com.utec.inventario.dto.response;

import java.util.List;

public record PaginaAuditoriaResponse(List<AuditoriaResponse> content, int number, int size,
        long totalElements, int totalPages, boolean first, boolean last) {
}
