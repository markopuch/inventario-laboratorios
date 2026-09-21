package com.utec.inventario.domain;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioLaboratorio {

    private Usuario usuario;
    private Laboratorio laboratorio;
    private boolean activo;
    private OffsetDateTime fechaAsignacion;
}

