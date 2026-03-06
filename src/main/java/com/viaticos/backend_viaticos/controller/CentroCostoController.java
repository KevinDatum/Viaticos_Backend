package com.viaticos.backend_viaticos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.entity.CentroCosto;
import com.viaticos.backend_viaticos.repository.CentroCostoRepository;

@RestController
@RequestMapping("/centros-costo")
@CrossOrigin(origins = "http://localhost:5173")
public class CentroCostoController {

    @Autowired
    private CentroCostoRepository centroCostoRepository;

    @GetMapping
    public ResponseEntity<List<CentroCosto>> listarTodos() {
        // Devuelve la lista completa de áreas de gasto desde la base de datos
        return ResponseEntity.ok(centroCostoRepository.findAll());
    }
}
