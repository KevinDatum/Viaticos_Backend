package com.viaticos.backend_viaticos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.entity.Pais;
import com.viaticos.backend_viaticos.repository.PaisRepository;
import com.viaticos.backend_viaticos.service.PaisService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/paises")
@CrossOrigin(origins = "http://localhost:5173")
public class PaisController {
    
    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private PaisService paisService;

    @GetMapping
    public List<Pais> listar() {
        return paisRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Pais> crearPais(@RequestBody Pais pais) {
        Pais nuevoPais = paisService.crearPais(pais);
        return ResponseEntity.ok(nuevoPais);
    }
    
}
