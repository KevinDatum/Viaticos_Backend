package com.viaticos.backend_viaticos.dto.request;

import lombok.Data;

@Data
public class LoginRequest {

    private String correo;

    private String password;
    
}
