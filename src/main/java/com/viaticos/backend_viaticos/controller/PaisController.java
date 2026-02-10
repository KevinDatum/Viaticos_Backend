package com.viaticos.backend_viaticos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.entity.Pais;
import com.viaticos.backend_viaticos.repository.PaisRepository;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/paises")
@CrossOrigin(origins = "http://localhost:5173")
public class PaisController {
    
    @Autowired
    private PaisRepository paisRepository;

    @GetMapping
    public List<Pais> listar() {
        return paisRepository.findAll();
    }
    
}
