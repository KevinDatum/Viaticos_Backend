package com.viaticos.backend_viaticos.dto.response;

public interface UsuarioAdminDTO {
    Long getIdUsuario();
    Long getIdEmpleado();

    String getNombreCompleto();
    String getCorreo();

    String getDepartamento();
    String getCargo();
    String getEmpresa();
    String getPais();

    String getRol();
    String getEstado();
}
