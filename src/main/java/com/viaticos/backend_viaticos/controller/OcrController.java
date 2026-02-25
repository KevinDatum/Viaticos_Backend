package com.viaticos.backend_viaticos.controller;

import com.viaticos.backend_viaticos.dto.response.FacturaExtractResponse;
import com.viaticos.backend_viaticos.entity.OcrJob;
import com.viaticos.backend_viaticos.enums.OcrJobStatus;
import com.viaticos.backend_viaticos.repository.OcrJobRepository;
import com.viaticos.backend_viaticos.service.FacturaSaveService;
import com.viaticos.backend_viaticos.service.OciObjectStorageService;
import com.viaticos.backend_viaticos.service.OcrJobProcessorService;// Ajusta el paquete
import com.viaticos.backend_viaticos.service.storage.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            String objectNameTemp = "temp/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            String objectNameWebp = storageService.generateObjectName("gastos") + ".webp";

            // 2) Subir original al bucket TEMP (Para el OCR de OCI)
            ociObjectStorageService.uploadToTempBucket(
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    objectNameTemp);

            // 3) Convertir a WEBP y subir al bucket FINAL (Para visualización)
            byte[] webpBytes = storageService.convertToWebpBytes(file);
            ociObjectStorageService.uploadToWebpBucket(
                    new java.io.ByteArrayInputStream(webpBytes),
                    webpBytes.length,
                    "image/webp",
                    objectNameWebp);

            // 4) Generar PAR WEBP para que el frontend pueda mostrar la imagen
            String parUrlWebp = ociObjectStorageService.generateParUrlWebp(objectNameWebp, 180);

            // 5) Crear y Guardar el registro del JOB en DB
            OcrJob job = new OcrJob();
            job.setIdEvento(idEvento);
            job.setIdUsuario(idUsuario);
            job.setObjectNameTemp(objectNameTemp);
            job.setObjectNameWebp(objectNameWebp);
            job.setStatus(OcrJobStatus.PENDING);

            job = ocrJobRepository.save(job);

            // 6) Lanzar el proceso asíncrono (OCR + GenAI)
            ocrJobProcessorService.processJob(job.getIdJob());

            return ResponseEntity.ok(Map.of(
                    "message", "Imagen procesada. OCR iniciado en segundo plano.",
                    "jobId", job.getIdJob(),
                    "objectNameWebp", objectNameWebp,
                    "parUrlWebp", parUrlWebp));

        } catch (Exception e) {
            log.error("Error en upload-temp: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error subiendo imagen: " + e.getMessage());
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
                        "errorMessage", job.getErrorMessage() != null ? job.getErrorMessage() : ""
                )))
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

            return ResponseEntity.ok(Map.of(
                    "message", "Factura guardada correctamente",
                    "idGasto", idGastoCreado));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al confirmar factura: " + e.getMessage());
        }
    }
}
