package com.viaticos.backend_viaticos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.entity.Empresa;
import com.viaticos.backend_viaticos.repository.EmpresaRepository;
import com.viaticos.backend_viaticos.service.EmpresaService;

@RestController
@RequestMapping("/empresas")
@CrossOrigin(origins = "http://localhost:5173")
public class EmpresaController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<List<Empresa>> listarTodas() {
        // Devuelve la lista completa de empresas desde la base de datos
        return ResponseEntity.ok(empresaRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Empresa> crearEmpresa(@RequestBody Empresa empresa) {
        // ✨ Llamamos al service para que guarde Y AUDITE
        Empresa nuevaEmpresa = empresaService.crearEmpresa(empresa);
        return ResponseEntity.ok(nuevaEmpresa);
    }
}
