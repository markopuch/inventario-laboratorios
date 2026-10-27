package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utec.inventario.dto.request.ActualizarLaboratoriosUsuarioRequest;
import com.utec.inventario.dto.response.UsuarioLaboratoriosResponse;
import com.utec.inventario.mapper.UsuarioLaboratorioMapper;
import com.utec.inventario.service.UsuarioLaboratorioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "Asignaciones", description = "Configuración administrativa de asignaciones explícitas Usuario–Laboratorio.")
@RestController
@RequestMapping("/api/admin/usuarios/{idUsuario}/laboratorios")
public class UsuarioLaboratorioController {

    private final UsuarioLaboratorioService asignacionService;
    private final UsuarioLaboratorioMapper mapper;

    @Autowired
    public UsuarioLaboratorioController(UsuarioLaboratorioService asignacionService,
            UsuarioLaboratorioMapper mapper) {
        this.asignacionService = asignacionService;
        this.mapper = mapper;
    }

    @Operation(summary = "Consultar las asignaciones explícitas de un usuario",
            description = "Solo ADMIN. Incluye usuarios existentes aunque estén inactivos. No equivale al alcance efectivo global especial de un destinatario ADMIN.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public UsuarioLaboratoriosResponse listarAsignacionesActivas(
            @Positive @PathVariable("idUsuario") Integer idUsuario) {
        return this.mapper.toResponse(this.asignacionService.listarAsignacionesActivas(idUsuario));
    }

    @Operation(summary = "Reemplazar las asignaciones de laboratorios",
            description = "Solo ADMIN. Sustituye el conjunto completo de forma atómica; una lista vacía retira todas las asignaciones. Valida cada laboratorio antes de cambiar datos y conserva la historia de los pares reactivados.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public UsuarioLaboratoriosResponse reemplazarAsignaciones(
            @Positive @PathVariable("idUsuario") Integer idUsuario,
            @Valid @RequestBody ActualizarLaboratoriosUsuarioRequest request) {
        return this.mapper.toResponse(
                this.asignacionService.reemplazarAsignaciones(idUsuario, request.getIdsLaboratorio()));
    }
}

