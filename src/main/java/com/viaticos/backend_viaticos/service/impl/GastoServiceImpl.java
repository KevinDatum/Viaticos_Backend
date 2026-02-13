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
    public Gasto guardarGasto(Gasto gasto) {

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
