package com.viaticos.backend_viaticos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.entity.Regla;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.repository.ReglaRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

import java.util.List;

@Service
public class ReglaService {

    @Autowired
    private ReglaRepository reglaRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Regla> listarTodas() {
        return reglaRepository.findAll();
    }

    @Transactional
    public Regla guardarRegla(Regla regla) {
        // Si no tiene ID, es una CREACIÓN nueva
        if (regla.getIdRegla() == null) {
            regla.setEstadoActivo(reglaRepository.count() == 0 ? 1 : 0);
            Regla guardada = reglaRepository.save(regla);
            registrarLog("CREACIÓN", "REGLA", guardada.getIdRegla(), "Registró un nuevo manual de políticas IA: " + guardada.getNombre());
            return guardada;
        } 
        
        // Si ya tiene ID, es una EDICIÓN (mantiene su estado activo original)
        Regla actualizada = reglaRepository.save(regla);
        registrarLog("EDICIÓN", "REGLA", actualizada.getIdRegla(), "Modificó las reglas de la política: " + actualizada.getNombre());
        return actualizada;
    }

    @Transactional
    public void activarRegla(Long idRegla) {
        // 1. Apagar la actual
        reglaRepository.findByEstadoActivo(1).ifPresent(reglaActiva -> {
            reglaActiva.setEstadoActivo(0);
            reglaRepository.save(reglaActiva);
        });

        // 2. Encender la nueva
        Regla nuevaActiva = reglaRepository.findById(idRegla)
                .orElseThrow(() -> new RuntimeException("Regla no encontrada"));
        nuevaActiva.setEstadoActivo(1);
        reglaRepository.save(nuevaActiva);

        // 🛡️ AUDITORÍA
        registrarLog("EDICIÓN", "REGLA", nuevaActiva.getIdRegla(), "Activó una nueva Política de Auditoría (El LLM ahora usa esta versión).");
    }

    private void registrarLog(String accion, String tabla, Long idRegistro, String descripcion) {
        LogAuditoria log = new LogAuditoria();
        log.setAccion(accion);
        log.setTablaAfectada(tabla);
        log.setIdRegistroAfectado(idRegistro);
        log.setDescripcion(descripcion);
        try {
            String correoEjecutor = SecurityContextHolder.getContext().getAuthentication().getName();
            usuarioRepository.findByCorreo(correoEjecutor).ifPresent(log::setUsuario);
        } catch (Exception e) {}
        logAuditoriaRepository.save(log);
    }
}