package com.viaticos.backend_viaticos.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viaticos.backend_viaticos.dto.response.ReporteTarjetaDTO;
import com.viaticos.backend_viaticos.dto.response.TarjetaDTO;
import com.viaticos.backend_viaticos.entity.Empleado;
import com.viaticos.backend_viaticos.entity.Pais;
import com.viaticos.backend_viaticos.entity.Tarjeta;
import com.viaticos.backend_viaticos.repository.EmpleadoRepository;
import com.viaticos.backend_viaticos.repository.PaisRepository;
import com.viaticos.backend_viaticos.repository.TarjetaRepository;

@Service
public class TarjetaService {

        @Autowired
        private TarjetaRepository tarjetaRepository;

        @Autowired
        private EmpleadoRepository empleadoRepository;

        @Autowired
        private PaisRepository paisRepository;

        @Autowired
        private AuditoriaService auditoriaService;

        private TarjetaDTO mapToDTO(Tarjeta tarjeta) {
                TarjetaDTO dto = new TarjetaDTO();
                dto.setIdTarjeta(tarjeta.getIdTarjeta());
                dto.setBanco(tarjeta.getBanco());
                dto.setUltimos4Digitos(tarjeta.getUltimos4Digitos());
                dto.setAlias(tarjeta.getAlias());
                dto.setEstado(tarjeta.getEstado());
                dto.setFechaExpedicion(tarjeta.getFechaExpedicion());

                // ✨ FIX: Usamos getNombre() para el País
                if (tarjeta.getPais() != null) {
                        dto.setIdPais(tarjeta.getPais().getIdPais());
                        dto.setNombrePais(tarjeta.getPais().getNombre());
                }

                // ✨ FIX: Usamos getNombre() y getApellido() para el Empleado
                if (tarjeta.getEmpleado() != null) {
                        dto.setIdEmpleado(tarjeta.getEmpleado().getIdEmpleado());
                        dto.setNombreEmpleado(
                                        tarjeta.getEmpleado().getNombre() + " " + tarjeta.getEmpleado().getApellido());
                }
                return dto;
        }

        public List<TarjetaDTO> obtenerTodas() {
                return tarjetaRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        public List<TarjetaDTO> obtenerPorEmpleado(Long idEmpleado) {
                return tarjetaRepository.findByEmpleado_IdEmpleadoAndEstado(idEmpleado, "ACTIVA")
                                .stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        // 1. Crear Tarjeta + AUDITORÍA (MODIFICADO PARA ASIGNACIÓN AUTOMÁTICA)
        public TarjetaDTO crearTarjeta(TarjetaDTO dto, Long idUsuarioAuditor) {
                Tarjeta tarjeta = new Tarjeta();
                tarjeta.setBanco(dto.getBanco());
                tarjeta.setUltimos4Digitos(dto.getUltimos4Digitos());
                tarjeta.setAlias(dto.getAlias());
                tarjeta.setEstado("ACTIVA");
                tarjeta.setFechaExpedicion(dto.getFechaExpedicion());

                Pais pais = paisRepository.findById(dto.getIdPais())
                                .orElseThrow(() -> new RuntimeException("País no encontrado"));
                tarjeta.setPais(pais);

                // ✨ SOLUCIÓN: VINCULACIÓN AUTOMÁTICA AL EMPLEADO
                if (dto.getIdEmpleado() != null) {
                        Empleado empleado = empleadoRepository.findById(dto.getIdEmpleado())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Empleado no encontrado con ID: " + dto.getIdEmpleado()));
                        tarjeta.setEmpleado(empleado);
                }

                Tarjeta guardada = tarjetaRepository.save(tarjeta);

                // Ajustamos el mensaje de auditoría dependiendo si se asignó o no
                String mensajeAuditoria = "Se agregó tarjeta " + dto.getBanco() + " terminada en "
                                + dto.getUltimos4Digitos()
                                + " al inventario.";
                if (dto.getIdEmpleado() != null) {
                        mensajeAuditoria = "El usuario registró y vinculó la tarjeta " + dto.getBanco()
                                        + " terminada en "
                                        + dto.getUltimos4Digitos() + " a su billetera.";
                }

                auditoriaService.registrarLog(
                                idUsuarioAuditor,
                                "CREACIÓN",
                                "TARJETA",
                                guardada.getIdTarjeta(),
                                mensajeAuditoria);

                return mapToDTO(guardada);
        }

        // 2. Asignar Tarjeta + AUDITORÍA
        public TarjetaDTO asignarTarjeta(Long idTarjeta, Long idEmpleado, Long idAdmin) {
                Tarjeta tarjeta = tarjetaRepository.findById(idTarjeta)
                                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada"));
                Empleado empleado = empleadoRepository.findById(idEmpleado)
                                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

                tarjeta.setEmpleado(empleado);
                Tarjeta actualizada = tarjetaRepository.save(tarjeta);

                // 🛡️ REGISTRO DE AUDITORÍA ACTIVO
                auditoriaService.registrarLog(
                                idAdmin,
                                "ASIGNACIÓN",
                                "TARJETA",
                                idTarjeta,
                                "Se asignó la tarjeta al empleado ID " + idEmpleado);

                return mapToDTO(actualizada);
        }

        // 3. Revocar Tarjeta + AUDITORÍA
        public TarjetaDTO revocarTarjeta(Long idTarjeta, Long idAdmin) {
                Tarjeta tarjeta = tarjetaRepository.findById(idTarjeta)
                                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada"));

                Long idEmpleadoAnterior = tarjeta.getEmpleado() != null ? tarjeta.getEmpleado().getIdEmpleado() : null;
                tarjeta.setEmpleado(null);
                Tarjeta actualizada = tarjetaRepository.save(tarjeta);

                // 🛡️ REGISTRO DE AUDITORÍA ACTIVO
                auditoriaService.registrarLog(
                                idAdmin,
                                "REVOCACIÓN",
                                "TARJETA",
                                idTarjeta,
                                "Se revocó la tarjeta al empleado ID " + idEmpleadoAnterior
                                                + ". Regresada a inventario.");

                return mapToDTO(actualizada);
        }

        // 4. Reportar Tarjeta (Robo/Extravío) + AUDITORÍA
        @Transactional
        public TarjetaDTO reportarTarjeta(Long idTarjeta, ReporteTarjetaDTO reporte) {
                // 1. Buscamos la tarjeta
                Tarjeta tarjeta = tarjetaRepository.findById(idTarjeta)
                                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada con ID: " + idTarjeta));

                // 2. Verificamos que el empleado que reporta sea el dueño actual
                if (tarjeta.getEmpleado() == null
                                || !tarjeta.getEmpleado().getIdEmpleado().equals(reporte.getIdEmpleado())) {
                        throw new RuntimeException(
                                        "Operación no autorizada: Solo el titular puede reportar esta tarjeta.");
                }

                String estadoAnterior = tarjeta.getEstado();

                // 3. Cambiamos el estado de la tarjeta
                // En tu UI tienes 3 colores (Activa, Bloqueada, Reportada).
                // Como es el empleado quien levanta la mano, el estado oficial será REPORTADA.
                tarjeta.setEstado("REPORTADA");

                Tarjeta actualizada = tarjetaRepository.save(tarjeta);

                // 4. 🛡️ REGISTRO DE AUDITORÍA
                // Usamos el mismo formato que ya usas para asignar/revocar
                String detalleAuditoria = String.format(
                                "Incidencia reportada. Estado cambió de %s a REPORTADA. Motivo: %s | Detalles: %s",
                                estadoAnterior, // ✨ AQUÍ LA ESTAMOS USANDO
                                reporte.getMotivo(),
                                (reporte.getComentario() != null && !reporte.getComentario().isEmpty()
                                                ? reporte.getComentario()
                                                : "Ninguno"));

                // Si idEmpleado también funge como idAdmin (el que hace la acción) en tu
                // AuditoriaService, pásalo así:
                auditoriaService.registrarLog(
                                reporte.getIdEmpleado(), // El ID de quien ejecuta la acción
                                "REPORTE_INCIDENCIA",
                                "TARJETA",
                                idTarjeta,
                                detalleAuditoria);

                return mapToDTO(actualizada);
        }

        // 5. Resolver Incidencia de Tarjeta (Admin) + AUDITORÍA
        @Transactional
        public TarjetaDTO resolverIncidencia(Long idTarjeta, String nuevoEstado, String resolucion, Long idAdmin) {
                Tarjeta tarjeta = tarjetaRepository.findById(idTarjeta)
                                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada"));

                String estadoAnterior = tarjeta.getEstado();

                // Actualizamos al estado que decida el admin (BLOQUEADA o ACTIVA)
                tarjeta.setEstado(nuevoEstado.toUpperCase());

                // Si se bloquea por robo, podríamos desvincularla del empleado,
                // pero es mejor mantenerla vinculada para el historial.
                // Depende de tu regla de negocio, por ahora solo cambiamos el estado.

                Tarjeta actualizada = tarjetaRepository.save(tarjeta);

                // 🛡️ REGISTRO DE AUDITORÍA
                String detalleAuditoria = String.format(
                                "Incidencia resuelta. Estado cambió de %s a %s. Resolución: %s",
                                estadoAnterior,
                                nuevoEstado.toUpperCase(),
                                (resolucion != null && !resolucion.isEmpty() ? resolucion
                                                : "Sin comentarios adicionales"));

                auditoriaService.registrarLog(
                                idAdmin,
                                "RESOLUCION_INCIDENCIA",
                                "TARJETA",
                                idTarjeta,
                                detalleAuditoria);

                return mapToDTO(actualizada);
        }

        // 6. Eliminar Tarjeta permanentemente + AUDITORÍA
        @Transactional
        public void eliminarTarjeta(Long idTarjeta, Long idUsuarioAuditor) {
                // 1. Buscamos la tarjeta o lanzamos error si no existe
                Tarjeta tarjeta = tarjetaRepository.findById(idTarjeta)
                                .orElseThrow(() -> new RuntimeException(
                                                "La tarjeta con ID " + idTarjeta + " no existe en la bóveda."));

                // ✨ ELIMINAMOS EL BLOQUEO DE SEGURIDAD
                // Ya no lanzamos excepción si tarjeta.getEmpleado() != null

                // 2. Capturamos los datos para el log antes de que desaparezcan de la DB
                String banco = tarjeta.getBanco();
                String ultimos4 = tarjeta.getUltimos4Digitos();

                // 3. Verificamos si tenía dueño para dejar el rastro en la auditoría
                String detalleAsignacion = "";
                if (tarjeta.getEmpleado() != null) {
                        detalleAsignacion = " (La tarjeta fue retirada abruptamente de la billetera de: "
                                        + tarjeta.getEmpleado().getNombre() + " " + tarjeta.getEmpleado().getApellido()
                                        + ").";
                }

                // 4. Ejecutamos la eliminación física
                try {
                        tarjetaRepository.delete(tarjeta);
                } catch (Exception e) {
                        // 🛡️ ÚNICO BLOQUEO REAL: Si la base de datos rechaza el borrado porque la
                        // tarjeta ya se usó para pagar viáticos.
                        throw new RuntimeException(
                                        "No se puede eliminar la tarjeta porque ya tiene historial de gastos asociados en el sistema. Debe desactivarla en su lugar.");
                }

                // 5. 🛡️ REGISTRO DE AUDITORÍA
                auditoriaService.registrarLog(
                                idUsuarioAuditor,
                                "ELIMINACIÓN",
                                "TARJETA",
                                idTarjeta,
                                "Se eliminó permanentemente la tarjeta " + banco + " terminada en " + ultimos4
                                                + " de la bóveda corporativa." + detalleAsignacion);
        }
}
