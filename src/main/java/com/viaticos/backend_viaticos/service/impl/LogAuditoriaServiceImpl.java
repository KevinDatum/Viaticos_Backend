package com.viaticos.backend_viaticos.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.service.LogAuditoriaService;

import jakarta.transaction.Transactional;

public class LogAuditoriaServiceImpl implements LogAuditoriaService {

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Override
    @Transactional
    public void registrarLog(String accion,
            String tabla,
            Long idRegistro,
            String campo,
            String valorAnterior,
            String valorNuevo,
            String justificacion,
            String descripcion,
            Usuario usuarioAccion,
            Usuario usuarioAfectado) {

        LogAuditoria log = new LogAuditoria();
        log.setAccion(accion);
        log.setTablaAfectada(tabla);
        log.setIdRegistroAfectado(idRegistro);
        log.setCampoAfectado(campo);
        log.setValorAnterior(valorAnterior);
        log.setValorNuevo(valorNuevo);
        log.setJustificacion(justificacion);
        log.setDescripcion(descripcion);
        log.setUsuario(usuarioAccion);
        log.setUsuarioAfectado(usuarioAfectado);

        logAuditoriaRepository.save(log);
    }

}
