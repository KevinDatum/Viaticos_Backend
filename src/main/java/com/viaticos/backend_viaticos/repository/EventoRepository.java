package com.viaticos.backend_viaticos.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.viaticos.backend_viaticos.dto.response.EventoDTO;
import com.viaticos.backend_viaticos.entity.Evento;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    @Query(value = "SELECT " +
            "e.id_evento AS idEvento, " +
            "e.nombre AS nombre, " +
            "e.fecha_inicio AS fechaInicio, " +
            "e.fecha_fin AS fechaFin, " +
            "(emp.nombre || ' ' || emp.apellido) AS responsable, " +
            "e.id_empleado AS idEmpleado, " +
            "e.estado AS estado, " +
            "e.presupuesto AS presupuesto, " + // <--- Verifica que esta coma esté ahí
            "NVL((SELECT SUM(g.monto_usd) FROM GASTO g " +
            "     WHERE g.id_evento = e.id_evento " +
            "     AND g.estado_Actual = 'APROBADO'), 0) AS totalGastado " +
            "FROM EVENTO e " +
            "JOIN EMPLEADO emp ON e.id_empleado = emp.id_empleado " +
            "ORDER BY e.fecha_inicio DESC", nativeQuery = true)
    List<EventoDTO> findAllEventosWithTotals();

    @Modifying
    @Query("""
                UPDATE Evento e
                SET e.estado = 'Finalizado'
                WHERE e.estado <> 'Finalizado'
                  AND e.fecha_fin < :hoy
            """)
    void finalizarEventosVencidos(@Param("hoy") LocalDate hoy);

    @Modifying
    @Query("""
                UPDATE Evento e
                SET e.estado = 'Activo'
                WHERE e.estado = 'Planificado'
                  AND e.fecha_inicio <= :hoy
                  AND e.fecha_fin >= :hoy
            """)
    void activarEventosHoy(@Param("hoy") LocalDate hoy);

    @Query(value = "SELECT " +
            "ev.id_evento AS \"idEvento\", " +
            "ev.nombre AS \"nombre\", " +
            "ev.fecha_inicio AS \"fechaInicio\", " +
            "ev.fecha_fin AS \"fechaFin\", " +
            "(emp.nombre || ' ' || emp.apellido) AS \"responsable\", " +
            "NVL((SELECT SUM(g.monto_usd) FROM GASTO g WHERE g.id_evento = ev.id_evento), 0) AS \"totalGastado\", " +
            "ev.estado AS \"estado\", " +
            "ev.id_empleado AS \"idEmpleado\", " +
            "ev.presupuesto AS \"presupuesto\" " +
            "FROM EVENTO ev " +
            "LEFT JOIN EMPLEADO emp ON ev.id_empleado = emp.id_empleado " +
            "WHERE ev.id_evento = :idEvento",
            nativeQuery = true)
    EventoDTO findEventoById(@Param("idEvento") Long idEvento);

}
