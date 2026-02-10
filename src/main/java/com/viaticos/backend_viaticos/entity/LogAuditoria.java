package com.viaticos.backend_viaticos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "LOG_AUDITORIA")
@Data
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long idLog;

    private String accion;

    @Column(name = "tabla_afectada")
    private String tablaAfectada;

    @Column(name = "id_registro_afectado")
    private Long idRegistroAfectado;

    @Column(name = "campo_afectado")
    private String campoAfectado;

    @Lob
    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Lob
    @Column(name = "valor_nuevo")
    private String valorNuevo;

    @Column(name = "justificacion", length = 1000)
    private String justificacion;

    @Column(name = "fecha_hora", insertable = false)
    private LocalDateTime fechaHora;

    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_usuario_afectado")
    private Usuario usuarioAfectado;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
}
