package com.viaticos.backend_viaticos.repository;

import com.viaticos.backend_viaticos.dto.response.GastoDTO;
import com.viaticos.backend_viaticos.entity.Gasto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

        @Query(value = "SELECT " +
                        "g.id_gasto AS \"idGasto\", " +
                        "g.nombre_comercio AS \"nombreComercio\", " +
                        "g.descripcion AS \"descripcion\", " +
                        "g.monto_usd AS \"montoUsd\", " +
                        "g.monto AS \"montoOriginal\", " +
                        "g.moneda AS \"moneda\", " +
                        "cg.nombre AS \"categoria\", " +   // <-- JOIN CON TABLA CATEGORIA
                        "g.metodo_pago AS \"metodoPago\", " +
                        "g.ultimos4tarjeta AS \"ultimos4Tarjeta\", " +
                        "TO_CHAR(g.fecha, 'YYYY-MM-DD') AS \"fecha\", " +
                        "g.estado_actual AS \"estadoActual\", " +
                        "g.url_imagen AS \"urlImagen\", " +
                        "CAST(u.id_usuario AS VARCHAR2(50)) AS \"userId\", " +
                        "(emp.nombre || ' ' || emp.apellido) AS \"userName\", " +
                        "dep.nombre AS \"userArea\", " +
                        "r.nombre AS \"userRole\", " +
                        "ev.id_evento AS \"idEvento\", " +
                        "ev.nombre AS \"eventName\", " +
                        "(SELECT MAX(e_aud.nombre) FROM GASTO_HISTORIAL h " +
                        " JOIN USUARIO u_aud ON h.id_usuario = u_aud.id_usuario " +
                        " JOIN EMPLEADO e_aud ON u_aud.id_empleado = e_aud.id_empleado " +
                        " WHERE h.id_gasto = g.id_gasto) AS \"auditBy\", " +
                        "(SELECT MAX(h.motivo || ': ' || h.comentario) FROM GASTO_HISTORIAL h " +
                        " WHERE h.id_gasto = g.id_gasto AND h.id_historial = " +
                        " (SELECT MAX(id_historial) FROM GASTO_HISTORIAL WHERE id_gasto = g.id_gasto)) AS \"auditReason\", " +
                        "(SELECT TO_CHAR(MAX(h.fecha_hora), 'DD/MM/YYYY HH24:MI') FROM GASTO_HISTORIAL h " +
                        " WHERE h.id_gasto = g.id_gasto) AS \"fechaHoraAuditoria\" " +
                        "FROM Gasto g " +
                        "LEFT JOIN Categoria_Gasto cg ON g.id_categoria = cg.id_categoria " + // <-- NUEVO JOIN
                        "LEFT JOIN Evento ev ON g.id_evento = ev.id_evento " +
                        "LEFT JOIN Empleado emp ON ev.id_empleado = emp.id_empleado " +
                        "LEFT JOIN Departamento dep ON emp.id_departamento = dep.id_departamento " +
                        "LEFT JOIN Usuario u ON u.id_empleado = emp.id_empleado " +
                        "LEFT JOIN ROL r ON u.id_rol = r.id_rol", nativeQuery = true)
        List<GastoDTO> findAllGastosWithDetails();

        @Query(value = "SELECT " +
                        "g.id_gasto AS \"idGasto\", " +
                        "g.nombre_comercio AS \"nombreComercio\", " +
                        "g.descripcion AS \"descripcion\", " +
                        "g.monto_usd AS \"montoUsd\", " +
                        "g.monto AS \"montoOriginal\", " +
                        "g.moneda AS \"moneda\", " +
                        "cg.nombre AS \"categoria\", " +   // <-- JOIN CON TABLA CATEGORIA
                        "g.metodo_pago AS \"metodoPago\", " +
                        "g.ultimos4tarjeta AS \"ultimos4Tarjeta\", " +
                        "TO_CHAR(g.fecha, 'YYYY-MM-DD') AS \"fecha\", " +
                        "g.estado_actual AS \"estadoActual\", " +
                        "g.url_imagen AS \"urlImagen\", " +
                        "CAST(u.id_usuario AS VARCHAR2(50)) AS \"userId\", " +
                        "(emp.nombre || ' ' || emp.apellido) AS \"userName\", " +
                        "dep.nombre AS \"userArea\", " +
                        "r.nombre AS \"userRole\", " +
                        "ev.id_evento AS \"idEvento\", " +
                        "ev.nombre AS \"eventName\", " +
                        "(SELECT MAX(e_aud.nombre) FROM GASTO_HISTORIAL h " +
                        " JOIN USUARIO u_aud ON h.id_usuario = u_aud.id_usuario " +
                        " JOIN EMPLEADO e_aud ON u_aud.id_empleado = e_aud.id_empleado " +
                        " WHERE h.id_gasto = g.id_gasto) AS \"auditBy\", " +
                        "(SELECT MAX(h.motivo || ': ' || h.comentario) FROM GASTO_HISTORIAL h " +
                        " WHERE h.id_gasto = g.id_gasto AND h.id_historial = " +
                        " (SELECT MAX(id_historial) FROM GASTO_HISTORIAL WHERE id_gasto = g.id_gasto)) AS \"auditReason\", " +
                        "(SELECT TO_CHAR(MAX(h.fecha_hora), 'DD/MM/YYYY HH24:MI') FROM GASTO_HISTORIAL h " +
                        " WHERE h.id_gasto = g.id_gasto) AS \"fechaHoraAuditoria\" " +
                        "FROM Gasto g " +
                        "LEFT JOIN Categoria_Gasto cg ON g.id_categoria = cg.id_categoria " + // <-- NUEVO JOIN
                        "LEFT JOIN Evento ev ON g.id_evento = ev.id_evento " +
                        "LEFT JOIN Empleado emp ON ev.id_empleado = emp.id_empleado " +
                        "LEFT JOIN Departamento dep ON emp.id_departamento = dep.id_departamento " +
                        "LEFT JOIN Usuario u ON u.id_empleado = emp.id_empleado " +
                        "LEFT JOIN Rol r ON u.id_rol = r.id_rol " +
                        "WHERE ev.id_evento = :idEvento", nativeQuery = true)
        List<GastoDTO> findGastosByEvento(@Param("idEvento") Long idEvento);
}