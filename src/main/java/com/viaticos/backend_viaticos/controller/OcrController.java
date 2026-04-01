package com.viaticos.backend_viaticos.controller;

import com.viaticos.backend_viaticos.dto.response.FacturaExtractResponse;
import com.viaticos.backend_viaticos.entity.OcrJob;
import com.viaticos.backend_viaticos.enums.OcrJobStatus;
import com.viaticos.backend_viaticos.repository.OcrJobRepository;
import com.viaticos.backend_viaticos.service.FacturaSaveService;
import com.viaticos.backend_viaticos.service.OciObjectStorageService;
import com.viaticos.backend_viaticos.service.OcrJobProcessorService;// Ajusta el paquete
import com.viaticos.backend_viaticos.service.SseNotificationService;
import com.viaticos.backend_viaticos.service.storage.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OciObjectStorageService ociObjectStorageService;
    private final OcrJobProcessorService ocrJobProcessorService;
    private final OcrJobRepository ocrJobRepository;
    private final StorageService storageService; // Para conversión WebP
    private final FacturaSaveService facturaSaveService;
    private final SseNotificationService sseNotificationService;

    /**
     * Paso 1: Recibe la imagen, la procesa (WebP) y lanza el Job de OCR asíncrono.
     */
    @PostMapping(value = "/upload-temp", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadTemp(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long idEvento,
            @RequestParam Long idUsuario) {
        try {
            log.info("Recibiendo archivo para procesar: {}", file.getOriginalFilename());

            // 1) Generar nombres de objetos
            String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento.tmp";
            boolean isPdf = originalFileName.toLowerCase().endsWith(".pdf") ||
                    (file.getContentType() != null && file.getContentType().contains("pdf"));

            String objectNameTemp = "temp/" + UUID.randomUUID() + "_" + originalFileName;

            // ✨ FIX: Si es PDF, lo guardamos como .pdf en el bucket final. Si no, lo
            // convertimos a .webp
            String extensionFinal = isPdf ? ".pdf" : ".webp";
            String objectNameFinal = storageService.generateObjectName("gastos") + extensionFinal;

            // 2) Subir original al bucket TEMP (Para el OCR de OCI)
            ociObjectStorageService.uploadToTempBucket(
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    objectNameTemp);

            // 3) Subir al bucket FINAL (Para visualización)
            if (isPdf) {
                // 📄 RUTA PDF: Subimos el PDF directo sin intentar convertirlo a imagen
                ociObjectStorageService.uploadToWebpBucket(
                        file.getInputStream(),
                        file.getSize(),
                        file.getContentType(),
                        objectNameFinal);
            } else {
                // 🖼️ RUTA IMAGEN: Convertimos a WEBP y subimos
                byte[] webpBytes = storageService.convertToWebpBytes(file);
                ociObjectStorageService.uploadToWebpBucket(
                        new java.io.ByteArrayInputStream(webpBytes),
                        webpBytes.length,
                        "image/webp",
                        objectNameFinal);
            }

            // 4) Generar PAR para que el frontend pueda mostrar el archivo (sea PDF o WEBP)
            String parUrlFinal = ociObjectStorageService.generateParUrlWebp(objectNameFinal, 180);

            // 5) Crear y Guardar el registro del JOB en DB
            OcrJob job = new OcrJob();
            job.setIdEvento(idEvento);
            job.setIdUsuario(idUsuario);
            job.setObjectNameTemp(objectNameTemp);
            // ✨ IMPORTANTE: Usamos el nombre final correcto (sea .pdf o .webp)
            job.setObjectNameWebp(objectNameFinal);
            job.setStatus(OcrJobStatus.PENDING);

            job = ocrJobRepository.save(job);

            // 6) Lanzar el proceso asíncrono (OCR + GenAI)
            ocrJobProcessorService.processJob(job.getIdJob());

            return ResponseEntity.ok(Map.of(
                    "message", "Archivo procesado. OCR iniciado en segundo plano.",
                    "jobId", job.getIdJob(),
                    "objectNameWebp", objectNameFinal, // Ahora puede ser .webp o .pdf
                    "parUrlWebp", parUrlFinal));

        } catch (Exception e) {
            log.error("Error en upload-temp: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error subiendo archivo: " + e.getMessage());
        }
    }

    /**
     * Paso 2: El frontend consulta el estado del Job hasta que pase a COMPLETED.
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable Long jobId) {
        return ocrJobRepository.findById(jobId)
                .<ResponseEntity<?>>map(job -> ResponseEntity.ok(Map.of(
                        "jobId", job.getIdJob(),
                        "status", job.getStatus().name(),
                        "resultJson", job.getResultJson() != null ? job.getResultJson() : "",
                        "errorMessage", job.getErrorMessage() != null ? job.getErrorMessage() : "")))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "Job no encontrado")));
    }

    /**
     * Paso 3: El usuario confirma los datos y se guardan definitivamente en la DB.
     */
    @PostMapping("/factura/confirmar")
    public ResponseEntity<?> confirmarYGuardarFactura(
            @RequestBody FacturaExtractResponse factura,
            @RequestParam Long idEvento,
            @RequestParam Long idUsuario,
            @RequestParam String objectNameWebp) {
        try {
            Long idGastoCreado = facturaSaveService.guardarFacturaConfirmada(
                    factura, idEvento, idUsuario, objectNameWebp);

            sseNotificationService.notificarCambioEnGastos();

            return ResponseEntity.ok(Map.of(
                    "message", "Factura guardada correctamente",
                    "idGasto", idGastoCreado));

        } catch (Exception e) {
            String mensajeError = e.getMessage();

            // ✨ VALIDACIÓN DE SEGURIDAD:
            // Si el mensaje contiene "Bloqueo", enviamos 409 (Conflicto de duplicidad)
            if (mensajeError != null && mensajeError.contains("¡Bloqueo")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", mensajeError));
            }

            // Para cualquier otro error, enviamos 400 pero siempre en formato JSON
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error al confirmar factura: " + mensajeError));
        }
    }

    @PostMapping(value = "/upload-dte")
    public ResponseEntity<?> uploadDteJson(
            @RequestBody String dteJsonContent, // Recibe el JSON crudo en el body
            @RequestParam Long idEvento,
            @RequestParam Long idUsuario) {
        try {
            log.info("Recibiendo Factura Electrónica (DTE) en formato JSON puro.");

            // 1) Al no haber imagen, usaremos un "placeholder" o null para los nombres de
            // objeto
            // Esto le indicará al frontend que no intente renderizar un WebP
            String objectNameTemp = "DTE_DIRECT_UPLOAD";
            String objectNameWebp = "NO_IMAGE_DTE.json"; // Vacío porque no hay imagen
            String parUrlWebp = ""; // Vacío porque no hay imagen

            // 2) Crear y Guardar el registro del JOB en DB
            OcrJob job = new OcrJob();
            job.setIdEvento(idEvento);
            job.setIdUsuario(idUsuario);
            job.setObjectNameTemp(objectNameTemp);
            job.setObjectNameWebp(objectNameWebp);
            job.setStatus(OcrJobStatus.PENDING);

            job = ocrJobRepository.save(job);

            // 3) Lanzar el proceso asíncrono EXPRESS (Directo al LLM)
            ocrJobProcessorService.processDteJob(job.getIdJob(), dteJsonContent);

            return ResponseEntity.ok(Map.of(
                    "message", "DTE recibido. Auditoría iniciada en segundo plano.",
                    "jobId", job.getIdJob(),
                    "objectNameWebp", objectNameWebp,
                    "parUrlWebp", parUrlWebp));

        } catch (Exception e) {
            log.error("Error en upload-dte: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error procesando el DTE: " + e.getMessage());
        }
    }
}
