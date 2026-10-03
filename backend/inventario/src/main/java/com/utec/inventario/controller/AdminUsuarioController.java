package com.utec.inventario.controller;

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

    @GetMapping
    public List<AdminUsuarioResponse> listarUsuarios() {
        return this.mapper.toAdminResponse(this.usuarioService.listarUsuarios());
    }

    @GetMapping("/{id}")
    public AdminUsuarioResponse obtenerUsuario(@PathVariable("id") Integer id) {
        return this.mapper.toAdminResponse(this.usuarioService.obtenerUsuario(id));
    }

    @PostMapping
    public ResponseEntity<AdminUsuarioResponse> crearUsuario(@Valid @RequestBody CreateUsuarioRequest request) {
        AdminUsuarioResponse response = this.mapper.toAdminResponse(
                this.usuarioService.crearUsuario(this.mapper.convert(request), request.getPassword()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public AdminUsuarioResponse actualizarUsuario(@PathVariable("id") Integer id,
            @Valid @RequestBody UpdateUsuarioRequest request) {
        return this.mapper.toAdminResponse(
                this.usuarioService.actualizarUsuario(id, this.mapper.convert(request)));
    }

    @PatchMapping("/{id}/estado")
    public AdminUsuarioResponse cambiarEstado(@PathVariable("id") Integer id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return this.mapper.toAdminResponse(this.usuarioService.cambiarEstado(id, request.getActivo()));
    }

    @PatchMapping("/{id}/rol")
    public AdminUsuarioResponse cambiarRol(@PathVariable("id") Integer id,
            @Valid @RequestBody CambiarRolUsuarioRequest request) {
        return this.mapper.toAdminResponse(this.usuarioService.cambiarRol(id, request.getRol()));
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@PathVariable("id") Integer id,
            @Valid @RequestBody CambiarPasswordUsuarioRequest request) {
        this.usuarioService.cambiarPassword(id, request.getPassword());
    }
}
