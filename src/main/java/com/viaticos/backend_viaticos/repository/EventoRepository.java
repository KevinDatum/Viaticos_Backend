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
      "e.presupuesto AS presupuesto, " +
      "e.fecha_limite_gastos AS fechaLimiteGastos, " +
      "e.extensiones_plazo AS extensionesPlazo, " +
      "p.nombre AS paisNombre, " +
      "e.motivo_viaje AS motivoViaje, " +                                          // ✅ NUEVO: Motivo
      "empresa.nombre AS empresaPago, " +                                          // ✅ NUEVO: Nombre Empresa
      "e.id_empresa_pago AS idEmpresaPago, " +                                     // ✅ NUEVO: ID Empresa
      "cc.nombre AS centroCostoNombre, " +                                         // ✅ NUEVO: Nombre Centro Costo
      "e.id_centro_costo AS idCentroCosto, " +                                     // ✅ NUEVO: ID Centro Costo
      "NVL((SELECT SUM(g.monto_usd) FROM GASTO g " +
      "     WHERE g.id_evento = e.id_evento " +
      "     AND g.estado_Actual = 'APROBADO'), 0) AS totalGastado " +
      "FROM EVENTO e " +
      "JOIN EMPLEADO emp ON e.id_empleado = emp.id_empleado " +
      "LEFT JOIN PAIS p ON e.id_pais = p.id_pais " + 
      "LEFT JOIN EMPRESA empresa ON e.id_empresa_pago = empresa.id_empresa " +     // ✅ NUEVO: Join Empresa
      "LEFT JOIN CENTRO_COSTO cc ON e.id_centro_costo = cc.id_centro_costo " +     // ✅ NUEVO: Join Centro Costo
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
      "ev.presupuesto AS \"presupuesto\", " +
      "ev.fecha_limite_gastos AS \"fechaLimiteGastos\", " +
      "ev.extensiones_plazo AS \"extensionesPlazo\", " +
      "p.nombre AS \"paisNombre\", " +  
      "ev.motivo_viaje AS \"motivoViaje\", " +                                       // ✅ NUEVO: Motivo (con comillas)
      "empresa.nombre AS \"empresaPago\", " +                                        // ✅ NUEVO: Nombre Empresa (con comillas)
      "ev.id_empresa_pago AS \"idEmpresaPago\", " +                                  // ✅ NUEVO: ID Empresa (con comillas)
      "cc.nombre AS \"centroCostoNombre\", " +                                       // ✅ NUEVO: Nombre Centro Costo (con comillas)
      "ev.id_centro_costo AS \"idCentroCosto\" " +                                   // ✅ NUEVO: ID Centro Costo (con comillas)
      "FROM EVENTO ev " +
      "LEFT JOIN EMPLEADO emp ON ev.id_empleado = emp.id_empleado " +
      "LEFT JOIN PAIS p ON ev.id_pais = p.id_pais " + 
      "LEFT JOIN EMPRESA empresa ON ev.id_empresa_pago = empresa.id_empresa " +     // ✅ NUEVO: Join Empresa
      "LEFT JOIN CENTRO_COSTO cc ON ev.id_centro_costo = cc.id_centro_costo " +     // ✅ NUEVO: Join Centro Costo
      "WHERE ev.id_evento = :idEvento", nativeQuery = true)
  EventoDTO findEventoById(@Param("idEvento") Long idEvento);

  @Query(value = "SELECT " +
      "e.id_evento AS idEvento, " +
      "e.nombre AS nombre, " +
      "e.fecha_inicio AS fechaInicio, " +
      "e.fecha_fin AS fechaFin, " +
      "(emp.nombre || ' ' || emp.apellido) AS responsable, " +
      "e.id_empleado AS idEmpleado, " +
      "e.estado AS estado, " +
      "e.presupuesto AS presupuesto, " +
      "e.fecha_limite_gastos AS fechaLimiteGastos, " +
      "e.extensiones_plazo AS extensionesPlazo, " +
      "p.nombre AS paisNombre, " +  
      "e.motivo_viaje AS motivoViaje, " +                                          // ✅ NUEVO: Motivo
      "empresa.nombre AS empresaPago, " +                                          // ✅ NUEVO: Nombre Empresa
      "e.id_empresa_pago AS idEmpresaPago, " +                                     // ✅ NUEVO: ID Empresa
      "cc.nombre AS centroCostoNombre, " +                                         // ✅ NUEVO: Nombre Centro Costo
      "e.id_centro_costo AS idCentroCosto, " +                                     // ✅ NUEVO: ID Centro Costo
      "NVL((SELECT SUM(g.monto_usd) FROM GASTO g " +
      "     WHERE g.id_evento = e.id_evento " +
      "     AND g.estado_Actual = 'APROBADO'), 0) AS totalGastado " +
      "FROM EVENTO e " +
      "JOIN EMPLEADO emp ON e.id_empleado = emp.id_empleado " +
      "LEFT JOIN PAIS p ON e.id_pais = p.id_pais " + 
      "LEFT JOIN EMPRESA empresa ON e.id_empresa_pago = empresa.id_empresa " +     // ✅ NUEVO: Join Empresa
      "LEFT JOIN CENTRO_COSTO cc ON e.id_centro_costo = cc.id_centro_costo " +     // ✅ NUEVO: Join Centro Costo
      "WHERE emp.id_departamento = (SELECT id_departamento FROM EMPLEADO WHERE id_empleado = :idGerente) " +
      "  AND emp.id_pais = (SELECT id_pais FROM EMPLEADO WHERE id_empleado = :idGerente) " +
      "ORDER BY e.fecha_inicio DESC", nativeQuery = true)
  List<EventoDTO> findAllEventosByGerente(@Param("idGerente") Long idGerente);

}
