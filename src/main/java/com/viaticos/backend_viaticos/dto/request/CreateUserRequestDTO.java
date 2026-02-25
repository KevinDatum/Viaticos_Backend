package com.viaticos.backend_viaticos.dto.request;

import lombok.Data;

@Data
public class CreateUserRequestDTO {
    private String nombre;
    private String apellido;
    private String correo;

    private Long idDepartamento;
    private Long idCargo;
    private Long idEmpresa;
    private Long idPais;

    private Long idJefe; // opcional

    private Long idRol;
}
