package com.viaticos.backend_viaticos.dto.request;
import lombok.Data;

@Data
public class UpdateUserRequestDTO {
    private Long idDepartamento;
    private Long idCargo;
    private Long idEmpresa;
    private Long idPais;
    private Long idRol;
    private Long idJefe; // Opcional
}
