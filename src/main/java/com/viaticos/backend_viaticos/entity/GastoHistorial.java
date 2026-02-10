package com.viaticos.backend_viaticos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "GASTO_HISTORIAL")
@Data
public class GastoHistorial {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @Column(name = "estado_anterior") // Mantenemos el typo del SQL
    private String estadoAnterior;
    
    @Column(name = "estado_nuevo")
    private String estadoNuevo;
    
    private String motivo;
    private String comentario;
    
    @Column(name = "fecha_hora", insertable = false)
    private LocalDateTime fechaHora;

    @ManyToOne
    @JoinColumn(name = "id_gasto")
    private Gasto gasto;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
}
