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
@Table(name = "REPORT_TEMPLATES")
@Data
public class ReportTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Lob
    @Column(name = "MAPPING_JSON")
    private String mappingJson;

    @Lob
    @Column(name = "EXCEL_FILE")
    private byte[] excelFile;

    @Column(name = "FECHA_CREACION")
    private LocalDateTime fechaCreacion;

    private Integer activo;
}
