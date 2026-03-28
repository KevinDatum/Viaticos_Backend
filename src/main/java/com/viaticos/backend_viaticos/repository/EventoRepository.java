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

    // 1. Listar todos (Admin)
    @Query(value = "SELECT * FROM VW_EVENTOS_DETALLE ORDER BY \"fechaInicio\" DESC", nativeQuery = true)
    List<EventoDTO> findAllEventosWithTotals();

    // 2. Obtener uno por ID
    @Query(value = "SELECT * FROM VW_EVENTOS_DETALLE WHERE \"idEvento\" = :idEvento", nativeQuery = true)
    EventoDTO findEventoById(@Param("idEvento") Long idEvento);

    // 3. Listar por Gerente
    @Query(value = "SELECT * FROM VW_EVENTOS_DETALLE " +
            "WHERE id_departamento = (SELECT id_departamento FROM EMPLEADO WHERE id_empleado = :idGerente) " +
            "AND id_pais_empleado = (SELECT id_pais FROM EMPLEADO WHERE id_empleado = :idGerente) " +
            "ORDER BY \"fechaInicio\" DESC", nativeQuery = true)
    List<EventoDTO> findAllEventosByGerente(@Param("idGerente") Long idGerente);

    // Mantenemos los métodos de actualización de estado (son @Modifying)
    @Modifying
    @Query("UPDATE Evento e SET e.estado = 'Finalizado' WHERE e.estado <> 'Finalizado' AND e.fecha_fin < :hoy")
    void finalizarEventosVencidos(@Param("hoy") LocalDate hoy);

    @Modifying
    @Query("UPDATE Evento e SET e.estado = 'Activo' WHERE e.estado = 'Planificado' AND e.fecha_inicio <= :hoy AND e.fecha_fin >= :hoy")
    void activarEventosHoy(@Param("hoy") LocalDate hoy);
}
