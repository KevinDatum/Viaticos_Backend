package com.viaticos.backend_viaticos.repository;

import com.viaticos.backend_viaticos.dto.response.GastoDTO;
import com.viaticos.backend_viaticos.entity.Gasto;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

        // 1. Listar todos (Admin)
        @Query(value = "SELECT * FROM VW_GASTOS_DETALLE", nativeQuery = true)
        List<GastoDTO> findAllGastosWithDetails();

        // 2. Filtrar por Evento
        @Query(value = "SELECT * FROM VW_GASTOS_DETALLE WHERE \"idEvento\" = :idEvento", nativeQuery = true)
        List<GastoDTO> findGastosByEvento(@Param("idEvento") Long idEvento);

        // 3. Suma optimizada (esta puede quedarse en la tabla base por velocidad de
        // índice)
        @Query(value = "SELECT NVL(SUM(monto_usd), 0) FROM GASTO WHERE id_evento = :idEvento AND estado_actual = 'APROBADO'", nativeQuery = true)
        BigDecimal sumGastosAprobadosByEvento(@Param("idEvento") Long idEvento);

        @Query(value = "SELECT COUNT(g.id_gasto) FROM GASTO g " +
                        "JOIN EVENTO e ON g.id_evento = e.id_evento " +
                        "WHERE g.fecha = :fecha " +
                        "AND g.monto = :monto " +
                        "AND e.id_empleado = :idEmpleado " +
                        "AND NVL(g.numero_factura, 'S/N') = NVL(:numFactura, 'S/N')", nativeQuery = true)
        long countDuplicates(
                        @Param("numFactura") String numFactura,
                        @Param("monto") java.math.BigDecimal monto,
                        @Param("fecha") java.time.LocalDate fecha,
                        @Param("idEmpleado") Long idEmpleado);
}