package com.viaticos.backend_viaticos.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "EVENTO")
@Data
public class Evento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long idEvento;

    private String nombre;

    private LocalDate fecha_inicio;

    private LocalDate fecha_fin;

    private BigDecimal presupuesto;

    private String estado;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @ManyToOne
    @JoinColumn(name = "id_pais")
    private Pais pais;

    @Column(name = "fecha_limite_gastos")
    private LocalDate fechaLimiteGastos;

    @Column(name = "extensiones_plazo")
    private Integer extensionesPlazo = 0; // Inicia en 0 por defecto

    @Column(name = "MOTIVO_VIAJE")
    private String motivoViaje;

    @ManyToOne
    @JoinColumn(name = "ID_EMPRESA_PAGO")
    private Empresa empresaPago; 

    @ManyToOne
    @JoinColumn(name = "ID_CENTRO_COSTO")
    private CentroCosto areaGasto;
}
