package com.viaticos.backend_viaticos.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.aidocument.AIServiceDocumentClient;
import com.oracle.bmc.aidocument.model.*;
import com.oracle.bmc.aidocument.requests.CreateProcessorJobRequest;
import com.oracle.bmc.aidocument.requests.GetProcessorJobRequest;
import com.oracle.bmc.aidocument.responses.GetProcessorJobResponse;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.viaticos.backend_viaticos.service.ocr.OcrProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;

@Slf4j
@Service("ociOcrProvider")
@RequiredArgsConstructor
public class OciOcrProviderImpl implements OcrProvider {

    private final AIServiceDocumentClient documentClient;
    private final ObjectStorageClient objectStorageClient; // Inyectamos para leer el resultado
    private final ObjectMapper objectMapper; // Para manejar el JSON de salida

    @Value("${oci.namespace}")
    private String namespace;

    @Value("${oci.bucket.temp.name}")
    private String tempBucketName;

    @Value("${app.compartmentId}")
    private String compartmentId;

    @Override
    public String process(String objectName) throws Exception {
        log.info("Iniciando Job OCR OCI para: {}", objectName);

        // 1. Configurar entrada
        InputLocation inputLocation = ObjectStorageLocations.builder()
                .objectLocations(Arrays.asList(ObjectLocation.builder()
                        .namespaceName(namespace)
                        .bucketName(tempBucketName)
                        .objectName(objectName)
                        .build()))
                .build();

        // 2. Configurar salida (OCI crea una estructura de carpetas dentro de este
        // prefix)
        String outputPrefix = "resultados-ocr";
        OutputLocation outputLocation = OutputLocation.builder()
                .namespaceName(namespace)
                .bucketName(tempBucketName)
                .prefix(outputPrefix)
                .build();

        // 3. Crear Job
        CreateProcessorJobRequest createRequest = CreateProcessorJobRequest.builder()
                .createProcessorJobDetails(CreateProcessorJobDetails.builder()
                        .compartmentId(compartmentId)
                        .inputLocation(inputLocation)
                        .outputLocation(outputLocation)
                        .processorConfig(GeneralProcessorConfig.builder()
                                .features(Arrays.asList(DocumentKeyValueExtractionFeature.builder().build()))
                                .build())
                        .build())
                .build();

        String jobId = documentClient.createProcessorJob(createRequest).getProcessorJob().getId();
        log.info("Job creado: {}. Esperando procesamiento...", jobId);

        // 4. Polling: Esperar a que el Job termine
        ProcessorJob.LifecycleState state = ProcessorJob.LifecycleState.InProgress;
        while (state == ProcessorJob.LifecycleState.InProgress || state == ProcessorJob.LifecycleState.Accepted) {
            Thread.sleep(2000); // Esperar 2 segundos entre consultas
            GetProcessorJobResponse jobResponse = documentClient.getProcessorJob(
                    GetProcessorJobRequest.builder().processorJobId(jobId).build());
            state = jobResponse.getProcessorJob().getLifecycleState();
        }

        if (state != ProcessorJob.LifecycleState.Succeeded) {
            throw new RuntimeException("El Job de OCI falló con estado: " + state);
        }

        // 5. Localizar y Leer el JSON de salida de forma dinámica
        // En lugar de adivinar el nombre, listamos el contenido de la carpeta del Job
        String jobFolder = outputPrefix + "/" + jobId + "/";
        log.info("Buscando resultado en la carpeta: {}", jobFolder);

        var listRequest = com.oracle.bmc.objectstorage.requests.ListObjectsRequest.builder()
                .namespaceName(namespace)
                .bucketName(tempBucketName)
                .prefix(jobFolder)
                .build();

        var listResponse = objectStorageClient.listObjects(listRequest);

        // Buscamos el primer objeto que termine en .json
        String actualResultPath = listResponse.getListObjects().getObjects().stream()
                .map(com.oracle.bmc.objectstorage.model.ObjectSummary::getName)
                .filter(name -> name.endsWith(".json"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró el archivo JSON de salida en OCI"));

        log.info("Archivo JSON localizado: {}", actualResultPath);

        // Ahora sí, leemos el archivo con la ruta real
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(tempBucketName)
                .objectName(actualResultPath)
                .build();

        try (InputStream is = objectStorageClient.getObject(getObjectRequest).getInputStream()) {
            // 1. Leemos el JSON completo en un árbol de nodos (Jackson)
            JsonNode fullJson = objectMapper.readTree(is);

            // 2. DESTILADOR: Extraemos solo lo relevante para ahorrar tokens
            StringBuilder textToProcess = new StringBuilder();

            // Extraer campos clave-valor encontrados (Ej: Total: 150.00)
            JsonNode keyValues = fullJson.at("/pages/0/documentFields");
            if (keyValues.isArray()) {
                textToProcess.append("--- CAMPOS DETECTADOS ---\n");
                for (JsonNode field : keyValues) {
                    String label = field.path("fieldLabel").path("name").asText();
                    String value = field.path("fieldValue").path("value").asText();
                    if (!label.isEmpty() && !value.isEmpty()) {
                        textToProcess.append(label).append(": ").append(value).append("\n");
                    }
                }
            }

            // Extraer todo el texto plano por si el OCR falló en las llaves
            textToProcess.append("\n--- TEXTO COMPLETO DE LA FACTURA ---\n");
            JsonNode lines = fullJson.at("/pages/0/lines");
            for (JsonNode line : lines) {
                textToProcess.append(line.path("text").asText()).append(" ");
            }

            // Retornamos solo el texto limpio (Cuesta 95% menos en el LLM)
            return textToProcess.toString();
        }
    }
}
