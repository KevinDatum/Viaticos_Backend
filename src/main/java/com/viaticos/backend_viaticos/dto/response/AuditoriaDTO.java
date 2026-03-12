package com.viaticos.backend_viaticos.dto.response;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditoriaDTO implements Comparable<AuditoriaDTO> {
    private String id;
    private String user;
    private String action;
    private String target;
    private String details;
    private String date;
    private String time;
    
    // Campo oculto solo para ordenar en Java de más reciente a más antiguo
    private LocalDateTime rawDate; 

    @Override
    public int compareTo(AuditoriaDTO o) {
        return o.getRawDate().compareTo(this.rawDate); // Orden descendente
    }
}