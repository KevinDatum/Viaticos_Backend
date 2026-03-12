package com.viaticos.backend_viaticos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viaticos.backend_viaticos.entity.CentroCosto;
import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.repository.CentroCostoRepository;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

@Service
public class CentroCostoService {

    @Autowired
    private CentroCostoRepository centroCostoRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public CentroCosto crearCentroCosto(CentroCosto centroData) {
        CentroCosto centroGuardado = centroCostoRepository.save(centroData);

        // 🛡️ AUDITORÍA
        LogAuditoria log = new LogAuditoria();
        log.setAccion("CREACIÓN");
        log.setTablaAfectada("CENTRO_COSTO");
        log.setIdRegistroAfectado(centroGuardado.getIdCentroCosto()); // Ajusta si tu getter es distinto
        log.setValorNuevo(centroGuardado.getNombre());
        log.setDescripcion("Registró nuevo centro de costo: " + centroGuardado.getNombre());
        
        try {
            String correoEjecutor = SecurityContextHolder.getContext().getAuthentication().getName();
            usuarioRepository.findByCorreo(correoEjecutor).ifPresent(log::setUsuario);
        } catch (Exception e) {}

        logAuditoriaRepository.save(log);
        return centroGuardado;
    }
}