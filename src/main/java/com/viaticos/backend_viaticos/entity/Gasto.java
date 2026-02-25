package com.viaticos.backend_viaticos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Gasto")
@Data
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Long idGasto;

    @Column(name = "id_categoria", nullable = false)
    private Long idCategoria;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "id_tarjeta")
    private Long idTarjeta;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "nombre_comercio", nullable = false, length = 250)
    private String nombreComercio;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "monto_usd", precision = 15, scale = 2)
    private BigDecimal montoUsd;

    @Column(name = "tasa_cambio", precision = 12, scale = 6)
    private BigDecimal tasaCambio;

    @Column(name = "fecha_tasa_cambio", nullable = false)
    private LocalDate fechaTasaCambio;

    @Column(name = "estado_actual", length = 50)
    private String estadoActual = "BORRADOR";

    @Column(name = "url_imagen", length = 1000)
    private String urlImagen;

    @Column(name = "estado_ia", length = 50)
    private String estadoIa;

    @Lob // Para el tipo CLOB de Oracle
    @Column(name = "raw_json_ocr")
    private String rawJsonOcr;

    @Lob
    @Column(name = "texto_ocr_limpio")
    private String textoOcrLimpio;

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "ultimos4tarjeta")
    private String ultimos4Tarjeta;

    @Column(name = "estado_procesamiento", length = 50)
    private String estadoProcesamiento; // ej: SUBIDO, PROCESANDO_OCR, LLM_LISTO, ERROR

    @Column(name = "fecha_upload")
    private LocalDateTime fechaUpload;
}
