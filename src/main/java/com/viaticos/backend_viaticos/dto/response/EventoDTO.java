package com.viaticos.backend_viaticos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface EventoDTO {

    Long getIdEvento();
    String getNombre();
    LocalDate getFechaInicio();
    LocalDate getFechaFin();
    String getResponsable(); // Nombre + Apellido del empleado
    BigDecimal getTotalGastado(); // Suma de todos los gastos asociados
    String getEstado();
    Long getIdEmpleado();

    BigDecimal getPresupuesto();

    LocalDate getFechaLimiteGastos();
    Integer getExtensionesPlazo();

    String getPaisNombre();

    String getMotivoViaje();
    String getEmpresaPago();       // Nombre de la empresa (Ej: "DATUM SV")
    Long getIdEmpresaPago();       // ID de la empresa (Útil si luego quieres editar el evento)
    String getCentroCostoNombre(); // Nombre del área de gasto (Ej: "120 FINANZAS")
    Long getIdCentroCosto();
    
}