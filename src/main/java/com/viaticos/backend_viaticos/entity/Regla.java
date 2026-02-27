package com.viaticos.backend_viaticos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "REGLA")
@Data
public class Regla {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REGLA")
    private Long idRegla;

    @Column(name = "NOMBRE", nullable = false)
    private String nombre;

    @Column(name = "VERSION", nullable = false)
    private String version;

    @Column(name = "ESTADO_ACTIVO")
    private Integer estadoActivo; // Usamos Integer porque Oracle maneja el 1 y 0

    @Lob
    @Column(name = "CONTENIDO_JSON", nullable = false)
    private String contenidoJson;

    @Column(name = "FECHA_REGISTRO", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
