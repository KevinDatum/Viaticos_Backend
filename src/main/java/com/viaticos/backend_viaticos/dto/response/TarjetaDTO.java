package com.viaticos.backend_viaticos.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TarjetaDTO {
    private Long idTarjeta;
    private String banco;
    private String ultimos4Digitos;
    private String alias;
    private String estado;
    private LocalDate fechaExpedicion;
    
    // Datos del país
    private Long idPais;
    private String nombrePais;
    
    // Datos del empleado (serán nulos si la tarjeta está libre en inventario)
    private Long idEmpleado;
    private String nombreEmpleado;
}
