package com.viaticos.backend_viaticos.scheduler;

import com.viaticos.backend_viaticos.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class EventoScheduler {

    @Autowired
    private EventoRepository eventoRepository;

     @Scheduled(cron = "1 0 0 * * *") // todos los días 00:00:01
    @Transactional
    public void actualizarEstadosEventos() {

        LocalDate hoy = LocalDate.now();

        // PLANIFICADO -> ACTIVO
        eventoRepository.activarEventosHoy(hoy);

        // ACTIVO -> FINALIZADO (y también PLANIFICADO si ya pasó la fechaFin)
        eventoRepository.finalizarEventosVencidos(hoy.minusDays(1));

        System.out.println("LOG: Scheduler ejecutado para la fecha: " + hoy);
    }
}
