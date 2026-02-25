package com.viaticos.backend_viaticos.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viaticos.backend_viaticos.dto.request.GastoItemOcrRequestDTO;
import com.viaticos.backend_viaticos.dto.response.FacturaExtractResponse;
import com.viaticos.backend_viaticos.entity.Gasto;
import com.viaticos.backend_viaticos.entity.GastoHistorial;
import com.viaticos.backend_viaticos.entity.GastoItem;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.GastoHistorialRepository;
import com.viaticos.backend_viaticos.repository.GastoItemRepository;
import com.viaticos.backend_viaticos.repository.GastoRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacturaSaveService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private GastoItemRepository gastoItemRepository;

    @Autowired
    private GastoHistorialRepository gastoHistorialRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public Long guardarFacturaConfirmada(FacturaExtractResponse factura,
            Long idEvento,
            Long idUsuario,
            String objectNameWebp) throws Exception {

        // Parsear fecha
        LocalDate fechaFactura = parseFechaFactura(factura.getGasto().getFecha());

        int anioActual = LocalDate.now().getYear();
        int anioFactura = fechaFactura.getYear();

        String estadoFinal = "PENDIENTE";
        String motivoHistorial = "CREADO";
        String comentarioHistorial = "Gasto guardado como pendiente";

        // Validación ticket de otro año
        if (anioFactura != anioActual) {
            estadoFinal = "RECHAZADO";
            motivoHistorial = "FECHA_FUERA_DE_ANIO";
            comentarioHistorial = "Ticket pertenece al año " + anioFactura + " y el año actual es " + anioActual;
        }

        // Categoría (por ahora hardcodeada para pruebas)
        Long idCategoria = mapCategoriaToId(factura.getGasto().getCategoria());

        // defaults
        BigDecimal tasaCambio = BigDecimal.ONE;
        BigDecimal montoUsd = factura.getGasto().getMonto();
        Long idTarjeta = null;

        // Crear gasto

        Gasto gasto = new Gasto();
        gasto.setIdEvento(idEvento);
        gasto.setIdCategoria(idCategoria);
        gasto.setIdTarjeta(idTarjeta);

        gasto.setFecha(fechaFactura);
        gasto.setMonto(factura.getGasto().getMonto());
        gasto.setNombreComercio(factura.getGasto().getNombreComercio());
        gasto.setDescripcion(factura.getGasto().getDescripcion());
        gasto.setMoneda(factura.getGasto().getMoneda());
        gasto.setMontoUsd(montoUsd);
        gasto.setTasaCambio(tasaCambio);
        gasto.setFechaTasaCambio(LocalDate.now());
        gasto.setEstadoActual(estadoFinal);
        gasto.setUrlImagen(objectNameWebp);
        gasto.setEstadoIa("OCR_CONFIRMADO");
        gasto.setRawJsonOcr(mapper.writeValueAsString(factura));

        gasto.setMetodoPago(factura.getGasto().getMetodoPago());
        gasto.setUltimos4Tarjeta(factura.getGasto().getUltimos4Tarjeta());

        gasto = gastoRepository.save(gasto);

        // Guardar items
        if (factura.getItems() != null) {
            for (GastoItemOcrRequestDTO itemDTO : factura.getItems()) {

                GastoItem item = new GastoItem();
                item.setGasto(gasto);
                item.setDescripcion(itemDTO.getDescripcion());

                // cantidad por defecto 1
                BigDecimal cantidad = itemDTO.getCantidad() != null ? itemDTO.getCantidad() : BigDecimal.ONE;
                item.setCantidad(cantidad);

                item.setPrecioUnitario(itemDTO.getPrecioUnitario());
                item.setTotalItem(itemDTO.getPrecioTotal());

                gastoItemRepository.save(item);
            }
        }

        // Buscar usuario real
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + idUsuario));

        // Historial
        GastoHistorial historial = new GastoHistorial();
        historial.setGasto(gasto);
        historial.setUsuario(usuario);

        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(estadoFinal);

        historial.setMotivo(motivoHistorial);
        historial.setComentario(comentarioHistorial);

        // NO seteamos fechaHora porque tu entidad dice insertable=false (Oracle lo pone
        // solo)

        gastoHistorialRepository.save(historial);

        return gasto.getIdGasto();
    }

    private LocalDate parseFechaFactura(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            throw new RuntimeException("La fecha es obligatoria");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
        return LocalDate.parse(fecha, formatter);
    }

    private Long mapCategoriaToId(String categoria) {
        if (categoria == null)
            return 1L;

        categoria = categoria.trim().toLowerCase();

        return switch (categoria) {
            case "alimentacion" -> 1L;
            case "transporte" -> 2L;
            case "hospedaje" -> 3L;
            default -> 4L;
        };
    }
}
