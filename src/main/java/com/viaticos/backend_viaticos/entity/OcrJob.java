package com.viaticos.backend_viaticos.entity;

import java.time.LocalDateTime;

import com.viaticos.backend_viaticos.enums.OcrJobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "OCR_JOB")
@Data
public class OcrJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_JOB")
    private Long idJob;

    @Column(name = "ID_EVENTO", nullable = false)
    private Long idEvento;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long idUsuario;

    @Column(name = "OBJECT_NAME_TEMP", nullable = false, length = 500)
    private String objectNameTemp;

    @Column(name = "OBJECT_NAME_WEBP", nullable = false, length = 500)
    private String objectNameWebp;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private OcrJobStatus status;

    @Column(name = "ERROR_MESSAGE", length = 2000)
    private String errorMessage;

    @Lob
    @Column(name = "RESULT_JSON")
    private String resultJson;

    @Column(name = "ID_GASTO")
    private Long idGasto;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "FINISHED_AT")
    private LocalDateTime finishedAt;
}
