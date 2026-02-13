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
    
}