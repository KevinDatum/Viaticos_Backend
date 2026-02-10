package com.viaticos.backend_viaticos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viaticos.backend_viaticos.dto.response.EmpleadoDTO;
import com.viaticos.backend_viaticos.service.EmpleadoService;
import com.viaticos.backend_viaticos.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/empleados")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/subordinados/{idGerente}")
    public List<EmpleadoDTO> obtenerSubordinados(@PathVariable Long idGerente) {

        return empleadoService.obtenerSubordinados(idGerente);
    }

    @GetMapping("/subordinados")
    public List<EmpleadoDTO> obtenerSubordinados(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token no enviado");
        }

        String token = authHeader.substring(7);

        Long idGerente = jwtService.extractIdEmpleado(token);

        return empleadoService.obtenerSubordinados(idGerente);
    }

}
