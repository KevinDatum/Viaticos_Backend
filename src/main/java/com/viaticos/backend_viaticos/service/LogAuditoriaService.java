package com.viaticos.backend_viaticos.service;

import com.viaticos.backend_viaticos.entity.Usuario;

public interface LogAuditoriaService {
    
    void registrarLog(
            String accion,
            String tabla,
            Long idRegistro,
            String campo,
            String valorAnterior,
            String valorNuevo,
            String justificacion,
            String descripcion,
            Usuario usuarioAccion,
            Usuario usuarioAfectado
    );
}
