package com.viaticos.backend_viaticos.dto.response;

public class UserDetailsDTO {
    private String token;

    public UserDetailsDTO() {
    }

    public UserDetailsDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
