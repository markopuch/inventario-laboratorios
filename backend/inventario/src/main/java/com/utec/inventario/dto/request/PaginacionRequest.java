package com.utec.inventario.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PaginacionRequest {
    @Min(value = 0, message = "page debe ser mayor o igual que cero")
    @Schema(description = "Página base cero. page o size activa la respuesta paginada; ambos omitidos conservan el arreglo.",
            minimum = "0", defaultValue = "0")
    private Integer page;

    @Min(value = 1, message = "size debe ser mayor que cero")
    @Max(value = 100, message = "size no puede superar 100")
    @Schema(description = "Elementos por página; máximo 100. Omitido junto a page utiliza 20.",
            minimum = "1", maximum = "100", defaultValue = "20")
    private Integer size;

    // Ausencia conserva el contrato anterior; un parámetro presente vacío es inválido.
    public void setPage(Integer page) {
        if (page == null) throw new IllegalArgumentException("page no puede estar vacío");
        this.page = page;
    }

    public void setSize(Integer size) {
        if (size == null) throw new IllegalArgumentException("size no puede estar vacío");
        this.size = size;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSolicitada() {
        return this.page != null || this.size != null;
    }

    public int paginaEfectiva() {
        return this.page == null ? 0 : this.page;
    }

    public int tamanoEfectivo() {
        return this.size == null ? 20 : this.size;
    }

    @AssertTrue(message = "La página solicitada supera el desplazamiento máximo admitido")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isDesplazamientoValido() {
        // JPA utiliza un desplazamiento INTEGER aunque Pageable lo represente como long.
        return (long) this.paginaEfectiva() * this.tamanoEfectivo() <= Integer.MAX_VALUE;
    }
}
