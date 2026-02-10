package com.viaticos.backend_viaticos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LoginResponse {
    
    private Long idUsuario;
    private Long idEmpleado;
    private String nombreCompleto;
    private String rol;
    private String area;
    private String correo;
    private String token; 
}
