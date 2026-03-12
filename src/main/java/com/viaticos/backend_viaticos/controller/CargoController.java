package com.viaticos.backend_viaticos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.viaticos.backend_viaticos.entity.Cargo;
import com.viaticos.backend_viaticos.repository.CargoRepository;

import java.util.List;

@RestController
@RequestMapping("/cargos")
@CrossOrigin(origins = "http://localhost:5173")
public class CargoController {

    @Autowired
    private CargoRepository cargoRepository;

    @GetMapping
    public ResponseEntity<List<Cargo>> listarCargos() {
        return ResponseEntity.ok(cargoRepository.findAll());
    }
}
