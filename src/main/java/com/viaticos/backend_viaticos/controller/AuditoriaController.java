package com.viaticos.backend_viaticos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.viaticos.backend_viaticos.service.AuditoriaService;

@RestController
@RequestMapping("/admin/auditoria")
@CrossOrigin(origins = "http://localhost:5173")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<?> obtenerBitacora() {
        return ResponseEntity.ok(auditoriaService.obtenerBitacoraCompleta());
    }
}
