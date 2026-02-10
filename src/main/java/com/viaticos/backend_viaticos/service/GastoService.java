package com.viaticos.backend_viaticos.service;

import com.viaticos.backend_viaticos.dto.response.GastoDTO;
import com.viaticos.backend_viaticos.entity.Gasto;
import java.util.List;

public interface GastoService {

    Gasto guardarGasto(Gasto gasto);

    List<GastoDTO> listarTodos();

    List<GastoDTO> listarPorEvento(Long idEvento);

    void actualizarEstado(Long idGasto, Long idUsuario, String nuevoEstado, String motivo, String comentario);
    
}
