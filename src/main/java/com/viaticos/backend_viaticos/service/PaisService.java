package com.viaticos.backend_viaticos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.entity.Pais;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.repository.PaisRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

@Service
public class PaisService {

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public Pais crearPais(Pais paisData) {
        Pais paisGuardado = paisRepository.save(paisData);

        // 🛡️ AUDITORÍA
        LogAuditoria log = new LogAuditoria();
        log.setAccion("CREACIÓN");
        log.setTablaAfectada("PAIS");
        log.setIdRegistroAfectado(paisGuardado.getIdPais()); // Ajusta si tu getter es distinto
        log.setValorNuevo(paisGuardado.getNombre());
        log.setDescripcion("Registró nuevo país: " + paisGuardado.getNombre());
        
        try {
            String correoEjecutor = SecurityContextHolder.getContext().getAuthentication().getName();
            usuarioRepository.findByCorreo(correoEjecutor).ifPresent(log::setUsuario);
        } catch (Exception e) {}

        logAuditoriaRepository.save(log);
        return paisGuardado;
    }
}
