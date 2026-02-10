package com.viaticos.backend_viaticos.service;

import java.util.List;

import com.viaticos.backend_viaticos.dto.request.EventoRequestDTO;
import com.viaticos.backend_viaticos.dto.request.EventoUpdateRequestDTO;
import com.viaticos.backend_viaticos.dto.response.EventoDTO;



public interface EventoService {

    List<EventoDTO> listarEventosConTotales();

    EventoDTO obtenerEventoPorId(Long idEvento);

    void finalizarEvento(Long id, Long idUsuario);

    void guardarEvento(EventoRequestDTO request, Long idUsuario);

    void actualizarEvento(Long id, EventoUpdateRequestDTO request, Long idUsuario);
}
