package com.viaticos.backend_viaticos.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.dto.response.EmpleadoDTO;
import com.viaticos.backend_viaticos.repository.EmpleadoRepository;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    public List<EmpleadoDTO> obtenerSubordinados(Long idGerente) {

        return empleadoRepository.obtenerSubordinados(idGerente);
    }
    
}
