package com.viaticos.backend_viaticos.dto.response;

import lombok.Data;

@Data
public class ReporteTarjetaDTO {
    private Long idEmpleado;
    private String motivo; // Ej: "EXTRAVÍO", "ROBO", etc.
    private String comentario; // Ej: "La dejé en el taxi..."
}
