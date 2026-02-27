package com.viaticos.backend_viaticos.service;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

//import org.springframework.cache.annotation.Cacheable;

@Slf4j
@Service
@RequiredArgsConstructor
public class OciObjectStorageService {

    private final ObjectStorageClient objectStorageClient;

    @Value("${oci.bucket.name}")
    private String bucketWebp;

    @Value("${oci.bucket.temp.name}")
    private String bucketTemp;

    @Value("${oci.namespace}")
    private String namespace;

    @Value("${oci.region}")
    private String region;

    // --- MÉTODOS DE SUBIDA ---

    public String uploadToWebpBucket(InputStream inputStream, long contentLength, String contentType, String objectName) {
        return upload(bucketWebp, inputStream, contentLength, contentType, objectName);
    }

    public String uploadToTempBucket(InputStream inputStream, long contentLength, String contentType, String objectName) {
        return upload(bucketTemp, inputStream, contentLength, contentType, objectName);
    }

    private String upload(String bucketName, InputStream inputStream, long contentLength, String contentType, String objectName) {
        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .contentLength(contentLength)
                .contentType(contentType)
                .putObjectBody(inputStream)
                .build();

        objectStorageClient.putObject(request);
        log.info("Archivo {} subido exitosamente al bucket {}", objectName, bucketName);
        return objectName;
    }

    // --- MÉTODOS DE LECTURA ---

    /**
     * Obtiene el flujo de datos de cualquier objeto (útil para leer el JSON del OCR).
     */
    public InputStream getObjectStream(String bucketName, String objectName) {
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .build();
        
        GetObjectResponse response = objectStorageClient.getObject(request);
        return response.getInputStream();
    }

    // --- MÉTODOS DE ACCESO PÚBLICO TEMPORAL (PAR) ---

    /**
     * Genera URL temporal para que el Frontend muestre la imagen WebP.
     * Mantenemos el Cacheable para evitar llamadas excesivas a OCI.
     */
    //@Cacheable(value = "parWebpUrls", key = "#objectName")
    public String generateParUrlWebp(String objectName, int minutesValid) {
        return generateParUrl(bucketWebp, objectName, minutesValid);
    }

    private String generateParUrl(String bucketName, String objectName, int minutesValid) {
        String parName = "par_" + UUID.randomUUID();

        CreatePreauthenticatedRequestDetails details = CreatePreauthenticatedRequestDetails.builder()
                .name(parName)
                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                .objectName(objectName)
                .timeExpires(java.util.Date.from(OffsetDateTime.now().plusMinutes(minutesValid).toInstant()))
                .build();

        CreatePreauthenticatedRequestRequest request = CreatePreauthenticatedRequestRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .createPreauthenticatedRequestDetails(details)
                .build();

        CreatePreauthenticatedRequestResponse response = objectStorageClient.createPreauthenticatedRequest(request);
        PreauthenticatedRequest par = response.getPreauthenticatedRequest();

        // Construcción dinámica basada en la región configurada
        return String.format("https://objectstorage.%s.oraclecloud.com%s", region, par.getAccessUri());
    }

    // --- MÉTODOS DE ELIMINACIÓN ---

    public void deleteFromTempBucket(String objectName) {
        delete(bucketTemp, objectName);
    }

    private void delete(String bucketName, String objectName) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .build();

        objectStorageClient.deleteObject(request);
        log.info("Objeto {} eliminado del bucket {}", objectName, bucketName);
    }
}