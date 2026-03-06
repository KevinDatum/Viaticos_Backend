package com.viaticos.backend_viaticos.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "CENTRO_COSTO")
@Data
public class CentroCosto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CENTRO_COSTO")
    private Long idCentroCosto;

    @Column(name = "NOMBRE")
    private String nombre;

    @Column(name = "DESCRIPCION")
    private String descripcion;
}
