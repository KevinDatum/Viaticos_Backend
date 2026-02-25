package com.viaticos.backend_viaticos.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class GastoItemOcrRequestDTO {
    
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
}
