package com.viaticos.backend_viaticos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.dto.response.AuditoriaDTO;
import com.viaticos.backend_viaticos.entity.GastoHistorial;
import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.GastoHistorialRepository;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private LogAuditoriaRepository logRepository;

    @Autowired
    private GastoHistorialRepository gastoHistorialRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;


    // ==========================================
    // 1. MÉTODO NUEVO PARA ESCRIBIR LOGS
    // ==========================================
    public void registrarLog(Long idUsuario, String accion, String tablaAfectada, Long idRegistroAfectado, String descripcion) {
        try {
            LogAuditoria log = new LogAuditoria();
            
            // Si hay un usuario (no fue el sistema automático)
            if (idUsuario != null) {
                Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
                log.setUsuario(usuario);
            }
            
            log.setAccion(accion); // Ej: "CREACION", "ASIGNACION", "REVOCACION"
            log.setTablaAfectada(tablaAfectada); // Ej: "TARJETA"
            log.setIdRegistroAfectado(idRegistroAfectado); // El ID de la tarjeta
            log.setDescripcion(descripcion); // "Se asignó la tarjeta a X"
            log.setFechaHora(LocalDateTime.now());
            
            logRepository.save(log);
        } catch (Exception e) {
            // Un error en el log no debería detener el flujo principal
            System.err.println("Error al registrar auditoría: " + e.getMessage());
        }
    }

    public List<AuditoriaDTO> obtenerBitacoraCompleta() {
        List<AuditoriaDTO> bitacora = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        // 1. EXTRAER LOGS DEL SISTEMA (Creaciones, Ediciones, etc.)
        List<LogAuditoria> logs = logRepository.findAll();
        for (LogAuditoria log : logs) {
            AuditoriaDTO dto = new AuditoriaDTO();
            dto.setId("LOG-" + log.getIdLog());
            
            // Asumiendo que el Usuario tiene relación con Empleado para sacar el nombre
            String nombreUsuario = (log.getUsuario() != null && log.getUsuario().getEmpleado() != null) 
                    ? log.getUsuario().getEmpleado().getNombre() + " " + log.getUsuario().getEmpleado().getApellido()
                    : "SISTEMA";
            dto.setUser(nombreUsuario);
            
            dto.setAction(log.getAccion().toUpperCase()); // Ej: "CREACIÓN", "EDICIÓN"
            dto.setTarget(log.getTablaAfectada() + " #" + log.getIdRegistroAfectado());
            dto.setDetails(log.getDescripcion());
            
            if(log.getFechaHora() != null) {
                dto.setDate(log.getFechaHora().format(dateFormatter));
                dto.setTime(log.getFechaHora().format(timeFormatter));
                dto.setRawDate(log.getFechaHora());
            }
            bitacora.add(dto);
        }

        // 2. EXTRAER HISTORIAL DE GASTOS (Aprobaciones, Rechazos)
        List<GastoHistorial> historiales = gastoHistorialRepository.findAll();
        for (GastoHistorial hist : historiales) {
            AuditoriaDTO dto = new AuditoriaDTO();
            dto.setId("GST-" + hist.getIdHistorial());
            
            String nombreUsuario = (hist.getUsuario() != null && hist.getUsuario().getEmpleado() != null) 
                    ? hist.getUsuario().getEmpleado().getNombre() + " " + hist.getUsuario().getEmpleado().getApellido()
                    : "SISTEMA";
            dto.setUser(nombreUsuario);

            // Traducir el estado nuevo a la Acción del Frontend
            String estado = hist.getEstadoNuevo().toLowerCase();
            if (estado.contains("aprobado")) dto.setAction("APROBACIÓN");
            else if (estado.contains("rechazado")) dto.setAction("RECHAZO");
            else dto.setAction("EDICIÓN");

            dto.setTarget("Gasto #" + hist.getGasto().getIdGasto());
            dto.setDetails(hist.getComentario() != null ? hist.getComentario() : hist.getMotivo());
            
            if(hist.getFechaHora() != null) {
                dto.setDate(hist.getFechaHora().format(dateFormatter));
                dto.setTime(hist.getFechaHora().format(timeFormatter));
                dto.setRawDate(hist.getFechaHora());
            }
            bitacora.add(dto);
        }

        // 3. ORDENAR POR FECHA (MÁS RECIENTES PRIMERO) Y DEVOLVER
        Collections.sort(bitacora);
        return bitacora;
    }
}
