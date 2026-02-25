package com.viaticos.backend_viaticos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.dto.request.CreateUserRequestDTO;
import com.viaticos.backend_viaticos.service.UsuarioAdminService;

@RestController
@RequestMapping("/admin/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioAdminController {
    
    @Autowired
    private UsuarioAdminService usuarioAdminService;

    @GetMapping
    public ResponseEntity<?> listarUsuarios() {
        return ResponseEntity.ok(usuarioAdminService.listarUsuariosAdmin());
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody CreateUserRequestDTO dto) {
        return ResponseEntity.ok(usuarioAdminService.crearUsuarioConEmpleado(dto));
    }

    @PutMapping("/{idUsuario}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(usuarioAdminService.cambiarEstado(idUsuario));
    }
}
