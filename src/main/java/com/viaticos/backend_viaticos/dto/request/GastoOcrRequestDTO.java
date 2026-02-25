package com.viaticos.backend_viaticos.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class GastoOcrRequestDTO {
    
    private String fecha;          // DD/MM/YY
    private String categoria;      // Alimentacion, Transporte, Hospedaje, Otros
    private String moneda;         // USD, CRC, etc
    private BigDecimal monto;      // total final
    private String nombreComercio; // marca o comercio

    private String descripcion;

    private String metodoPago;
    private String ultimos4Tarjeta;
}
