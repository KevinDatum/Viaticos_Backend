package com.viaticos.backend_viaticos.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viaticos.backend_viaticos.dto.request.GastoItemOcrRequestDTO;
import com.viaticos.backend_viaticos.dto.response.EventoDTO;
import com.viaticos.backend_viaticos.dto.response.FacturaExtractResponse;
import com.viaticos.backend_viaticos.entity.Gasto;
import com.viaticos.backend_viaticos.entity.GastoHistorial;
import com.viaticos.backend_viaticos.entity.GastoItem;
import com.viaticos.backend_viaticos.entity.Usuario;
import com.viaticos.backend_viaticos.repository.EventoRepository;
import com.viaticos.backend_viaticos.repository.GastoHistorialRepository;
import com.viaticos.backend_viaticos.repository.GastoItemRepository;
import com.viaticos.backend_viaticos.repository.GastoRepository;
import com.viaticos.backend_viaticos.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private EventoRepository eventoRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional(rollbackFor = Exception.class)
    public Long guardarFacturaConfirmada(FacturaExtractResponse factura,
            Long idEvento,
            Long idUsuario,
            String objectNameWebp) throws Exception {

        // Parsear fecha
        LocalDate fechaFactura = parseFechaFactura(factura.getGasto().getFecha());

        // Valores por defecto
        String estadoFinal = "PENDIENTE";
        String motivoHistorial = "CREADO";
        String comentarioHistorial = "Gasto guardado sin auditoría IA";

        // --- 🤖 CAPTURAR DECISIÓN DE LA IA Y DEL FRONTEND ANTI-FRAUDE ---
        if (factura.getAuditoria() != null && factura.getAuditoria().getEstado_ia() != null) {
            estadoFinal = factura.getAuditoria().getEstado_ia().toUpperCase();
            motivoHistorial = "AUDITORIA_IA";
            comentarioHistorial = factura.getAuditoria().getMotivo_ia();

            if (comentarioHistorial != null && comentarioHistorial.contains("Alerta de Integridad")) {
                motivoHistorial = "ALERTA_INTEGRIDAD";
            }

            if (estadoFinal.equals("REVISION_GERENTE") ||
                    (!estadoFinal.equals("APROBADO") && !estadoFinal.equals("RECHAZADO"))) {
                estadoFinal = "PENDIENTE";
            }
        }

        // Solo verificamos si la IA intentaba aprobarlo automáticamente
        if (estadoFinal.equals("APROBADO")) {
            // Buscamos el presupuesto asignado al evento
            EventoDTO eventoActual = eventoRepository.findEventoById(idEvento);

            if (eventoActual != null && eventoActual.getPresupuesto() != null
                    && eventoActual.getPresupuesto().compareTo(BigDecimal.ZERO) > 0) {

                // Sumamos lo que ya está aprobado + el ticket que estamos intentando guardar
                // ahorita
                BigDecimal gastadoHastaAhora = gastoRepository.sumGastosAprobadosByEvento(idEvento);
                BigDecimal montoNuevoDolares = factura.getGasto().getMontoUsd() != null
                        ? BigDecimal.valueOf(factura.getGasto().getMontoUsd())
                        : factura.getGasto().getMonto();

                BigDecimal proyeccionTotal = gastadoHastaAhora.add(montoNuevoDolares);

                // Si se pasa del límite, la IA pierde su poder de aprobar
                if (proyeccionTotal.compareTo(eventoActual.getPresupuesto()) > 0) {
                    estadoFinal = "PENDIENTE";
                    motivoHistorial = "PRESUPUESTO_EXCEDIDO";
                    comentarioHistorial = "El gasto es válido, pero sobrepasa el presupuesto límite asignado al evento ($"
                            + eventoActual.getPresupuesto() + "). Requiere aprobación manual del Gerente.";
                }
            }
        }

        // Categoría
        Long idCategoria = mapCategoriaToId(factura.getGasto().getCategoria());

        // Extraemos los cálculos internacionales enviados por React
        BigDecimal tasaCambio = factura.getGasto().getTasaCambio() != null
                ? BigDecimal.valueOf(factura.getGasto().getTasaCambio())
                : BigDecimal.ONE;

        BigDecimal montoUsd = factura.getGasto().getMontoUsd() != null
                ? BigDecimal.valueOf(factura.getGasto().getMontoUsd())
                : factura.getGasto().getMonto();
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

    @Transactional(rollbackFor = Exception.class)
    public Long reEvaluarGastoRechazado(Long idGasto, Long idUsuario, FacturaExtractResponse factura) throws Exception {

        // 1. Buscar el gasto existente
        Gasto gasto = gastoRepository.findById(idGasto)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + idGasto));

        if ("APROBADO".equalsIgnoreCase(gasto.getEstadoActual())) {
            throw new RuntimeException("No se puede modificar un gasto que ya ha sido aprobado.");
        }

        String estadoAnterior = gasto.getEstadoActual();

        // 2. Parsear nueva fecha y extraer datos
        LocalDate fechaFactura = parseFechaFactura(factura.getGasto().getFecha());

        // --- 🛡️ MOTOR ANTI-FRAUDE EN EL BACKEND ---
        String estadoFinal = "APROBADO";
        String motivoHistorial = "RE_EVALUADO";
        String comentarioHistorial = "El usuario corrigió los datos y pasaron la validación.";

        // 1. Verificamos si el usuario alteró campos sensibles
        BigDecimal montoNuevo = factura.getGasto().getMonto() != null ? factura.getGasto().getMonto() : BigDecimal.ZERO;
        String comercioNuevo = factura.getGasto().getNombreComercio() != null
                ? factura.getGasto().getNombreComercio().trim()
                : "";

        boolean isMontoAlterado = gasto.getMonto() != null && montoNuevo.compareTo(gasto.getMonto()) != 0;
        boolean isComercioAlterado = gasto.getNombreComercio() != null
                && !comercioNuevo.equalsIgnoreCase(gasto.getNombreComercio().trim());

        if (isMontoAlterado || isComercioAlterado) {
            // 🚨 ALERTA: Pierde el derecho a aprobación automática, se va a PENDIENTE para
            // que el gerente lo vea
            estadoFinal = "PENDIENTE";
            motivoHistorial = "ALERTA_INTEGRIDAD";
            comentarioHistorial = "El empleado alteró el monto original o el comercio extraído por la IA. Requiere validación visual contra el ticket.";
        } else if (factura.getAuditoria() != null && factura.getAuditoria().getEstado_ia() != null) {
            estadoFinal = factura.getAuditoria().getEstado_ia().toUpperCase();
            comentarioHistorial = "Re-evaluado por IA: " + factura.getAuditoria().getMotivo_ia();

            // ✨ TRADUCCIÓN A TUS 3 ESTADOS OFICIALES
            if (estadoFinal.equals("REVISION_GERENTE") ||
                    (!estadoFinal.equals("APROBADO") && !estadoFinal.equals("RECHAZADO"))) {
                estadoFinal = "PENDIENTE";
            }
        }


        // --- 🛡️ ESCUDO: CONTROL DE PRESUPUESTO EXCEDIDO (TAMBIÉN AL EDITAR) ---
        if (estadoFinal.equals("APROBADO")) {
            EventoDTO eventoActual = eventoRepository.findEventoById(gasto.getIdEvento());

            if (eventoActual != null && eventoActual.getPresupuesto() != null
                    && eventoActual.getPresupuesto().compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal gastadoHastaAhora = gastoRepository.sumGastosAprobadosByEvento(gasto.getIdEvento());
                BigDecimal montoNuevoDolares = factura.getGasto().getMontoUsd() != null
                        ? BigDecimal.valueOf(factura.getGasto().getMontoUsd())
                        : factura.getGasto().getMonto();

                BigDecimal proyeccionTotal = gastadoHastaAhora.add(montoNuevoDolares);

                if (proyeccionTotal.compareTo(eventoActual.getPresupuesto()) > 0) {
                    estadoFinal = "PENDIENTE";
                    motivoHistorial = "PRESUPUESTO_EXCEDIDO";
                    comentarioHistorial = "El gasto corregido es válido, pero sobrepasa el presupuesto límite asignado al evento ($"
                            + eventoActual.getPresupuesto() + "). Requiere aprobación manual del Gerente.";
                }
            }
        }

        // 3. Actualizar la Cabecera del Gasto
        Long idCategoria = mapCategoriaToId(factura.getGasto().getCategoria());
        gasto.setIdCategoria(idCategoria);
        gasto.setFecha(fechaFactura);
        gasto.setMonto(factura.getGasto().getMonto());

        // ✨ Inyectamos la tasa y los dólares recalculados por React
        if (factura.getGasto().getMontoUsd() != null) {
            gasto.setMontoUsd(BigDecimal.valueOf(factura.getGasto().getMontoUsd()));
        } else {
            gasto.setMontoUsd(factura.getGasto().getMonto());
        }

        if (factura.getGasto().getTasaCambio() != null) {
            gasto.setTasaCambio(BigDecimal.valueOf(factura.getGasto().getTasaCambio()));
        }
        gasto.setNombreComercio(factura.getGasto().getNombreComercio());
        gasto.setDescripcion(factura.getGasto().getDescripcion());
        gasto.setMoneda(factura.getGasto().getMoneda());
        gasto.setMetodoPago(factura.getGasto().getMetodoPago());
        gasto.setUltimos4Tarjeta(factura.getGasto().getUltimos4Tarjeta());
        gasto.setEstadoActual(estadoFinal);

        gasto = gastoRepository.save(gasto);

        // 4. Reemplazar los Items (Productos)
        gastoItemRepository.deleteByGastoId(idGasto);

        if (factura.getItems() != null) {
            for (GastoItemOcrRequestDTO itemDTO : factura.getItems()) {
                GastoItem item = new GastoItem();
                item.setGasto(gasto);
                item.setDescripcion(itemDTO.getDescripcion());

                BigDecimal cantidad = itemDTO.getCantidad() != null ? itemDTO.getCantidad() : BigDecimal.ONE;
                item.setCantidad(cantidad);

                BigDecimal precioUnit = itemDTO.getPrecioUnitario() != null ? itemDTO.getPrecioUnitario()
                        : BigDecimal.ZERO;
                BigDecimal precioTot = itemDTO.getPrecioTotal() != null ? itemDTO.getPrecioTotal() : precioUnit;

                item.setPrecioUnitario(precioUnit);
                item.setTotalItem(precioTot);

                gastoItemRepository.save(item);
            }
        }

        // 5. Guardar en el Historial
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + idUsuario));

        GastoHistorial historial = new GastoHistorial();
        historial.setGasto(gasto);
        historial.setUsuario(usuario);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoFinal);
        historial.setMotivo(motivoHistorial);
        historial.setComentario(comentarioHistorial);

        gastoHistorialRepository.save(historial);

        return gasto.getIdGasto();
    }
}
