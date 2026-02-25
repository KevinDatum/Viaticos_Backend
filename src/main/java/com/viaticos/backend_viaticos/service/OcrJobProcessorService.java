package com.viaticos.backend_viaticos.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viaticos.backend_viaticos.dto.response.FacturaExtractResponse;
import com.viaticos.backend_viaticos.entity.OcrJob;
import com.viaticos.backend_viaticos.enums.OcrJobStatus;
import com.viaticos.backend_viaticos.repository.OcrJobRepository;
import com.viaticos.backend_viaticos.service.ocr.OcrProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrJobProcessorService {

    private final OcrJobRepository ocrJobRepository;
    private final OcrProvider ocrProvider; // Inyecta OciOcrProviderImpl
    private final ObjectMapper objectMapper;
    private final OciGenAiService ociGenAiService; 

    @Async("ocrExecutor")
    public void processJob(Long jobId) {
        OcrJob job = ocrJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("OCR_JOB no encontrado: " + jobId));

        try {
            // 1. Actualizar estado a PROCESANDO
            job.setStatus(OcrJobStatus.PROCESSING);
            ocrJobRepository.save(job);

            log.info("--- Fase 1: Ejecutando OCI Document Understanding (Extracción) ---");
            // El provider ahora devuelve el texto destilado (campos clave + texto plano)
            String distilledText = ocrProvider.process(job.getObjectNameTemp());

            log.info("--- Fase 2: Refinando datos con OCI Generative AI (LLM) ---");
            // Enviamos el texto limpio al LLM de OCI
            FacturaExtractResponse facturaFinal = ociGenAiService.parseOcrJson(distilledText);

            // 3. Serializar y guardar el resultado final
            String finalJson = objectMapper.writeValueAsString(facturaFinal);
            
            job.setResultJson(finalJson);
            job.setStatus(OcrJobStatus.COMPLETED);
            job.setFinishedAt(LocalDateTime.now());

            ocrJobRepository.save(job);
            log.info("¡Éxito! Job {} completado y datos estructurados guardados.", jobId);

        } catch (Exception ex) {
            log.error("Fallo crítico en el Job {}: {}", jobId, ex.getMessage());
            
            job.setStatus(OcrJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            ocrJobRepository.save(job);
        }
    }
}