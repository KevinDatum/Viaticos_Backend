package com.viaticos.backend_viaticos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viaticos.backend_viaticos.entity.Empresa;
import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.repository.EmpresaRepository;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public Empresa crearEmpresa(Empresa empresaData) {
        Empresa empresaGuardada = empresaRepository.save(empresaData);

        // 🛡️ AUDITORÍA
        LogAuditoria log = new LogAuditoria();
        log.setAccion("CREACIÓN");
        log.setTablaAfectada("EMPRESA");
        log.setIdRegistroAfectado(empresaGuardada.getIdEmpresa()); // Ajusta el get ID si es distinto
        log.setValorNuevo(empresaGuardada.getNombre());
        log.setDescripcion("Registró nueva empresa: " + empresaGuardada.getNombre());
        
        try {
            String correoEjecutor = SecurityContextHolder.getContext().getAuthentication().getName();
            usuarioRepository.findByCorreo(correoEjecutor).ifPresent(log::setUsuario);
        } catch (Exception e) {}

        logAuditoriaRepository.save(log);
        return empresaGuardada;
    }
}
