package com.viaticos.backend_viaticos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsuarioCreadoResponseDTO {
    
    private Long idUsuario;
    private Long idEmpleado;

    private String nombreCompleto;
    private String correo;

    private String departamento;
    private String cargo;
    private String empresa;
    private String pais;

    private String rol;
    private String estado;

    private String passwordTemporal;
}
