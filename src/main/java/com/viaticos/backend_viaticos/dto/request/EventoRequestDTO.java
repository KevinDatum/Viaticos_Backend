package com.viaticos.backend_viaticos.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class EventoRequestDTO {
    
    private String nombre;
    private Long idEmpleado;
    private Long idPais;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal presupuesto;
}
