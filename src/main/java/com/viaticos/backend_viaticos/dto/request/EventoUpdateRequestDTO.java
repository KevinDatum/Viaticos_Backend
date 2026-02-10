package com.viaticos.backend_viaticos.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class EventoUpdateRequestDTO {
    
    private LocalDate fechaFin;

    private BigDecimal presupuesto;

    private String motivoCambio;
}
