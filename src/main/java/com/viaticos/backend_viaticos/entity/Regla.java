package com.viaticos.backend_viaticos.entity;

import java.math.BigDecimal;

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
@Table(name = "REGLA")
@Data
public class Regla {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Long idRegla;

    @Column(name = "nombre_regla")
    private String nombreRegla;
    
    @Column(name = "monto_maximo")
    private BigDecimal montoMaximo;
    
    @Column(name = "mensaje_error")
    private String mensajeError;

    @ManyToOne
    @JoinColumn(name = "id_pais")
    private Pais pais;

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private CategoriaGasto categoria;
}
