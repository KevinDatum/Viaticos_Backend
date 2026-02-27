package com.viaticos.backend_viaticos.dto.response;

import java.math.BigDecimal;

/**
 * Usamos una Interfaz (Proyección) para que Spring mapee 
 * automáticamente los resultados de la Native Query de SQL.
 */
public interface GastoDTO {
    
    // Los nombres deben coincidir EXACTAMENTE con los alias del SQL
    Long getIdGasto();
    
    String getNombreComercio();
    
    String getDescripcion();
    
    BigDecimal getMontoUsd();
    
    // Usamos Object por si Oracle devuelve Timestamp en lugar de Date
    String getFecha(); 
    
    String getEstadoActual();
    
    String getUrlImagen();
    
    String getUserId(); 
    
    String getUserName(); 
    
    String getUserArea(); 

    String getUserRole();

    Long getIdEvento();
    
    String getEventName();

    String getAuditBy();
    String getAuditReason();

    String getFechaHoraAuditoria();

    String getFechaUpload();


    String getCategoria();
    String getMoneda();
    String getMetodoPago();
    String getUltimos4Tarjeta();
    BigDecimal getMontoOriginal();

    // Métodos default para compatibilidad con tu diseño de React
    default BigDecimal getMonto() { return getMontoUsd(); }
    default BigDecimal getTasaCambio() { return java.math.BigDecimal.ONE; }
}