package com.viaticos.backend_viaticos.service.impl;

import com.viaticos.backend_viaticos.dto.response.GastoDTO;
import com.viaticos.backend_viaticos.entity.Gasto;
import com.viaticos.backend_viaticos.entity.GastoHistorial;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.GastoHistorialRepository;
import com.viaticos.backend_viaticos.repository.GastoRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;
import com.viaticos.backend_viaticos.service.GastoService;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GastoServiceImpl implements GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private GastoHistorialRepository historialRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public boolean existeDuplicado(String numFactura, java.math.BigDecimal monto, java.time.LocalDate fecha, Long idEmpleado) {
        
        // 1. REGLA DE ORO: Si el ticket TIENE un número de factura válido (No es S/N)
        if (numFactura != null && !numFactura.trim().isEmpty() && !numFactura.equalsIgnoreCase("S/N")) {
            // Buscamos SOLO por número de factura. ¡El monto y la fecha no importan!
            // Si el número 020477 ya existe, es fraude garantizado.
            return gastoRepository.existsByNumeroFactura(numFactura.trim());
        } 
        
        // 2. CASO EXTREMO: El ticket no tiene número (Es S/N). 
        // Aquí SÍ tenemos que usar la fecha y el empleado para intentar adivinar si es duplicado.
        else {
            if (fecha == null || idEmpleado == null || monto == null) return false;
            long count = gastoRepository.countDuplicates("S/N", monto, fecha, idEmpleado);
            return count > 0;
        }
    }

    @Override
    @Transactional
    public Gasto guardarGasto(Gasto gasto) {
        String numFactura = gasto.getNumeroFactura();

        // ✨ REGLA DE NEGOCIO: Validar facturas duplicadas globales
        if (numFactura != null && !numFactura.trim().isEmpty() && !numFactura.equalsIgnoreCase("S/N")) {
            
            if (gasto.getIdGasto() == null) {
                // RUTA A: Es un gasto TOTALMENTE NUEVO
                if (gastoRepository.existsByNumeroFactura(numFactura.trim())) {
                    throw new RuntimeException("Posible Fraude: El número de factura '" + numFactura + "' ya fue ingresado previamente en el sistema.");
                }
            } else {
                // RUTA B: Es una ACTUALIZACIÓN (Re-evaluar gasto)
                // Ignoramos su propio ID para que no se bloquee a sí mismo
                if (gastoRepository.existsByNumeroFacturaAndIdGastoNot(numFactura.trim(), gasto.getIdGasto())) {
                    throw new RuntimeException("Posible Fraude: El número de factura '" + numFactura + "' ya está asociado a otro gasto diferente.");
                }
            }
        }

        // Si pasa el escudo, lo guardamos en Oracle
        return gastoRepository.save(gasto);
    }

    @Override
    public List<GastoDTO> listarTodos() {
        return gastoRepository.findAllGastosWithDetails();
    }

    @Override
    public List<GastoDTO> listarPorEvento(Long idEvento) {
        return gastoRepository.findGastosByEvento(idEvento);
    }

    @Transactional
    @Override
    public void actualizarEstado(Long idGasto, Long idUsuario, String nuevoEstado, String motivo, String comentario) {
        Gasto gasto = gastoRepository.findById(idGasto)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));

        Usuario auditor = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("ID de Usuario auditor no encontrado en Oracle: " + idUsuario));

        String estadoAnterior = gasto.getEstadoActual();

        // 1. Actualizar Gasto
        gasto.setEstadoActual(nuevoEstado.toUpperCase());
        gastoRepository.save(gasto);

        // 2. Crear Historial
        GastoHistorial historial = new GastoHistorial();
        historial.setGasto(gasto);
        historial.setUsuario(auditor);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado.toUpperCase());
        historial.setMotivo(motivo);
        historial.setComentario(comentario);

        // Forzamos la fecha manualmente por si la base de datos no tiene el DEFAULT
        historial.setFechaHora(java.time.LocalDateTime.now());

        historialRepository.save(historial);
    }

    @Transactional
    @Override
    public void actualizarImagen(Long idGasto, String objectName) {

        Gasto gasto = gastoRepository.findById(idGasto)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + idGasto));

        gasto.setUrlImagen(objectName);

        gastoRepository.save(gasto);
    }

    @Override
    public String obtenerObjectNameImagen(Long idGasto) {

        Gasto gasto = gastoRepository.findById(idGasto)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + idGasto));

        return gasto.getUrlImagen(); // aquí debe estar guardado el objectName
    }

}
