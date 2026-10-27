package com.utec.inventario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.utec.inventario.dto.request.CambiarEstadoUsuarioRequest;
import com.utec.inventario.dto.request.CambiarPasswordUsuarioRequest;
import com.utec.inventario.dto.request.CambiarRolUsuarioRequest;
import com.utec.inventario.dto.request.CreateUsuarioRequest;
import com.utec.inventario.dto.request.UpdateUsuarioRequest;
import com.utec.inventario.dto.response.AdminUsuarioResponse;
import com.utec.inventario.mapper.UsuarioMapper;
import com.utec.inventario.service.AdminUsuarioService;

import jakarta.validation.Valid;

@Tag(name = "Usuarios", description = "Administración exclusiva de ADMIN; no existe registro público ni CRUD de roles.")
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final AdminUsuarioService usuarioService;
    private final UsuarioMapper mapper;

    @Autowired
    public AdminUsuarioController(AdminUsuarioService usuarioService, UsuarioMapper mapper) {
        this.usuarioService = usuarioService;
        this.mapper = mapper;
    }

    @Operation(summary = "Listar usuarios administrativos",
            description = "Solo ADMIN, alcance global. Incluye cuentas activas e inactivas y devuelve datos públicos, sin contraseñas ni hashes.")
    @GetMapping
    public List<AdminUsuarioResponse> listarUsuarios() {
        return this.mapper.toAdminResponse(this.usuarioService.listarUsuarios());
    }

    @Operation(summary = "Consultar un usuario administrativo",
            description = "Solo ADMIN, alcance global. Permite consultar una cuenta existente, incluso inactiva, por su ID.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @GetMapping("/{id}")
    public AdminUsuarioResponse obtenerUsuario(@PathVariable("id") Integer id) {
        return this.mapper.toAdminResponse(this.usuarioService.obtenerUsuario(id));
    }

    @Operation(summary = "Crear un usuario",
            description = "Solo ADMIN. Crea una cuenta con rol ADMIN, GESTOR o LECTOR. Username y email son únicos incluso para inactivos. La contraseña exige al menos 15 caracteres Unicode y hasta 72 bytes UTF-8.")
    @ApiResponse(responseCode = "201", description = "Usuario creado",
            content = @Content(schema = @Schema(implementation = AdminUsuarioResponse.class)),
            headers = @Header(name = "Location", description = "URI del recurso creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PostMapping
    public ResponseEntity<AdminUsuarioResponse> crearUsuario(@Valid @RequestBody CreateUsuarioRequest request) {
        AdminUsuarioResponse response = this.mapper.toAdminResponse(
                this.usuarioService.crearUsuario(this.mapper.convert(request), request.getPassword()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Actualizar los datos públicos de un usuario",
            description = "Solo ADMIN. Reemplaza nombre, apellido, email y cargo. No admite cambiar username, rol, actividad o contraseña mediante este contrato.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PutMapping("/{id}")
    public AdminUsuarioResponse actualizarUsuario(@PathVariable("id") Integer id,
            @Valid @RequestBody UpdateUsuarioRequest request) {
        return this.mapper.toAdminResponse(
                this.usuarioService.actualizarUsuario(id, this.mapper.convert(request)));
    }

    @Operation(summary = "Activar o desactivar un usuario",
            description = "Solo ADMIN. Cambia la actividad sin borrar la cuenta; no permite desactivar al último ADMIN activo.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/estado")
    public AdminUsuarioResponse cambiarEstado(@PathVariable("id") Integer id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return this.mapper.toAdminResponse(this.usuarioService.cambiarEstado(id, request.getActivo()));
    }

    @Operation(summary = "Cambiar el rol de un usuario",
            description = "Solo ADMIN. Asigna uno de los tres roles existentes y protege al último ADMIN activo frente a una degradación.")
    @ApiResponse(responseCode = "200", description = "Respuesta obtenida correctamente", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    @PatchMapping("/{id}/rol")
    public AdminUsuarioResponse cambiarRol(@PathVariable("id") Integer id,
            @Valid @RequestBody CambiarRolUsuarioRequest request) {
        return this.mapper.toAdminResponse(this.usuarioService.cambiarRol(id, request.getRol()));
    }

    @Operation(summary = "Restablecer la contraseña de un usuario",
            description = "Solo ADMIN. Exige al menos 15 caracteres Unicode y hasta 72 bytes UTF-8, sin recortar la clave. No invalida automáticamente los JWT ya emitidos.")
    @ApiResponse(responseCode = "204", description = "Contraseña restablecida; sin cuerpo", content = @Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@PathVariable("id") Integer id,
            @Valid @RequestBody CambiarPasswordUsuarioRequest request) {
        this.usuarioService.cambiarPassword(id, request.getPassword());
    }
}
