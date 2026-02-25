package com.viaticos.backend_viaticos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JefeDTO {
    private Long idEmpleado;
    private String nombreCompleto;
    private String correo;
}
