package com.viaticos.backend_viaticos.dto.response;

public interface EmpleadoDTO {
    
    Long getIdEmpleado();
    String getNombre();
    String getApellido();
    String getCorreo();

    Long getIdDepartamento();
    String getDepartamento();

    Long getIdCargo();
    String getCargo();
}
