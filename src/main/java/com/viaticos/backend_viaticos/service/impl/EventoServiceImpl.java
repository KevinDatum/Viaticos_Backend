package com.viaticos.backend_viaticos.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.viaticos.backend_viaticos.dto.request.EventoRequestDTO;
import com.viaticos.backend_viaticos.dto.request.EventoUpdateRequestDTO;
import com.viaticos.backend_viaticos.dto.response.EventoDTO;
import com.viaticos.backend_viaticos.entity.Empleado;
import com.viaticos.backend_viaticos.entity.Evento;
import com.viaticos.backend_viaticos.entity.LogAuditoria;
import com.viaticos.backend_viaticos.entity.Pais;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.EmpleadoRepository;
import com.viaticos.backend_viaticos.repository.EventoRepository;
import com.viaticos.backend_viaticos.repository.LogAuditoriaRepository;
import com.viaticos.backend_viaticos.repository.PaisRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;
import com.viaticos.backend_viaticos.service.EventoService;

import jakarta.transaction.Transactional;

@Service
public class EventoServiceImpl implements EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<EventoDTO> listarEventosConTotales() {
        return eventoRepository.findAllEventosWithTotals();
    }

    @Override
    public EventoDTO obtenerEventoPorId(Long idEvento) {

        EventoDTO evento = eventoRepository.findEventoById(idEvento);

        if (evento == null) {
            throw new RuntimeException("No se encontró el evento con id: " + idEvento);
        }

        return evento;
    }

    @Override
    @Transactional
    public void finalizarEvento(Long idEvento, Long idUsuario) {

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no válido"));

        String estadoAnterior = evento.getEstado();

        evento.setEstado("Finalizado");
        eventoRepository.save(evento);

        LogAuditoria log = new LogAuditoria();
        log.setAccion("FINALIZAR_EVENTO");
        log.setTablaAfectada("EVENTO");
        log.setIdRegistroAfectado(evento.getIdEvento());
        log.setCampoAfectado("ESTADO");
        log.setValorAnterior(estadoAnterior);
        log.setValorNuevo("Finalizado");
        log.setJustificacion("Finalización manual del evento");
        log.setUsuario(usuario);

        logAuditoriaRepository.save(log);
    }

    @Override
    @Transactional
    public void guardarEvento(EventoRequestDTO request, Long idUsuario) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no válido"));

        Evento evento = new Evento();

        evento.setNombre(request.getNombre());
        evento.setFecha_inicio(request.getFechaInicio());
        evento.setFecha_fin(request.getFechaFin());
        evento.setPresupuesto(request.getPresupuesto());
        evento.setEstado("Planificado");
        evento.setFechaRegistro(LocalDateTime.now());

        Empleado empleado = empleadoRepository.findById(request.getIdEmpleado())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        Pais pais = paisRepository.findById(request.getIdPais())
                .orElseThrow(() -> new RuntimeException("Pais no encontrado"));

        evento.setEmpleado(empleado);
        evento.setPais(pais);

        Evento eventoGuardado = eventoRepository.save(evento);

        // ==============================
        // AUDITORÍA CREACIÓN EVENTO
        // ==============================
        LogAuditoria log = new LogAuditoria();
        log.setAccion("CREAR_EVENTO");
        log.setTablaAfectada("EVENTO");
        log.setIdRegistroAfectado(eventoGuardado.getIdEvento());
        log.setCampoAfectado("CREACION");
        log.setValorAnterior(null);
        log.setValorNuevo("Evento creado: " + eventoGuardado.getNombre());
        log.setJustificacion("Creación de evento");
        log.setDescripcion(
                "Se creó el evento '" + eventoGuardado.getNombre() +
                        "' con fecha inicio: " + eventoGuardado.getFecha_inicio() +
                        ", fecha fin: " + eventoGuardado.getFecha_fin() +
                        ", presupuesto: " + eventoGuardado.getPresupuesto() +
                        ", estado: " + eventoGuardado.getEstado());

        log.setUsuario(usuario);

        // usuario afectado: empleado asignado al evento (si quieres guardar el usuario
        // afectado)
        // (solo si ya tienes el campo usuarioAfectado en LogAuditoria)
        // log.setUsuarioAfectado(null); // opcional

        logAuditoriaRepository.save(log);
    }

    @Override
    @Transactional
    public void actualizarEvento(Long id, EventoUpdateRequestDTO requestUpdate, Long idUsuario) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no valido"));

        // Valores anteriores
        LocalDate fechaFinAnterior = evento.getFecha_fin();
        BigDecimal presupuestoAnterior = evento.getPresupuesto();

        // Validación: fecha fin no puede ser anterior al inicio
        if (requestUpdate.getFechaFin().isBefore(evento.getFecha_inicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior al inicio.");
        }

        boolean cambioFecha = !requestUpdate.getFechaFin().equals(fechaFinAnterior);
        boolean cambioPresupuesto = requestUpdate.getPresupuesto().compareTo(presupuestoAnterior) != 0;

        // Si hay cambios importantes, motivo obligatorio
        if ((cambioFecha || (cambioPresupuesto && requestUpdate.getPresupuesto().compareTo(presupuestoAnterior) > 0))) {
            if (requestUpdate.getMotivoCambio() == null || requestUpdate.getMotivoCambio().trim().isEmpty()) {
                throw new RuntimeException("Debe ingresar un motivo para realizar este cambio.");
            }
        }

        // -------------------------------
        // AUDITORÍA CAMBIO FECHA FIN
        // -------------------------------
        if (cambioFecha) {
            LogAuditoria log = new LogAuditoria();
            log.setAccion("CAMBIO_FECHA_FIN");
            log.setTablaAfectada("EVENTO");
            log.setIdRegistroAfectado(evento.getIdEvento());
            log.setCampoAfectado("FECHA_FIN");
            log.setValorAnterior(fechaFinAnterior.toString());
            log.setValorNuevo(requestUpdate.getFechaFin().toString());
            log.setJustificacion(requestUpdate.getMotivoCambio());
            log.setUsuario(usuario);

            logAuditoriaRepository.save(log);
        }

        // -------------------------------
        // AUDITORÍA CAMBIO PRESUPUESTO
        // -------------------------------
        if (cambioPresupuesto) {
            LogAuditoria log = new LogAuditoria();
            log.setAccion("CAMBIO_PRESUPUESTO");
            log.setTablaAfectada("EVENTO");
            log.setIdRegistroAfectado(evento.getIdEvento());
            log.setCampoAfectado("PRESUPUESTO");
            log.setValorAnterior(presupuestoAnterior.toString());
            log.setValorNuevo(requestUpdate.getPresupuesto().toString());

            // si sube presupuesto: obligatorio (ya validado arriba)
            // si baja presupuesto: opcional pero si viene, se guarda
            log.setJustificacion(
                    requestUpdate.getMotivoCambio() != null ? requestUpdate.getMotivoCambio()
                            : "Actualización de presupuesto");

            log.setUsuario(usuario);

            logAuditoriaRepository.save(log);
        }

        // -------------------------------
        // ACTUALIZAR EVENTO
        // -------------------------------
        evento.setFecha_fin(requestUpdate.getFechaFin());
        evento.setPresupuesto(requestUpdate.getPresupuesto());

        eventoRepository.save(evento);
    }

}
