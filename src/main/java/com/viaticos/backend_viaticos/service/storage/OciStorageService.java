package com.viaticos.backend_viaticos.service.storage;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.HeadObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;

@Service
public class OciStorageService {

    private final ObjectStorageClient client;

    @Value("${oci.namespace}")
    private String namespace;

    @Value("${oci.bucket.name}")
    private String bucketName;

    @Value("${oci.region}")
    private String region;

    public OciStorageService(ObjectStorageClient client) {
        this.client = client;
    }

    /**
     * Sube un archivo al bucket (byte[]).
     */
    public void uploadObject(byte[] bytes, String objectName, String contentType) {

        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("No hay contenido para subir.");
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .putObjectBody(new ByteArrayInputStream(bytes))
                .build();

        client.putObject(request);
    }

    /**
     * Elimina un objeto del bucket.
     */
    public void deleteObject(String objectName) {

        if (objectName == null || objectName.isBlank()) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .build();

        client.deleteObject(request);
    }

    /**
     * Verifica si existe un objeto en el bucket.
     */
    public boolean existsObject(String objectName) {

        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .build();

            HeadObjectResponse response = client.headObject(request);
            return response != null;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Genera un PAR URL temporal para lectura.
     */
    @Cacheable(value = "parUrls", key = "#objectName")
    public String generateParUrl(String objectName, int minutesValid) {

        System.out.println("⚠️ Generando PAR URL nuevo para: " + objectName);

        String parName = "par_" + UUID.randomUUID();

        CreatePreauthenticatedRequestDetails details = CreatePreauthenticatedRequestDetails.builder()
                .name(parName)
                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                .objectName(objectName)
                .timeExpires(Date.from(
                        OffsetDateTime.now().plusMinutes(minutesValid).toInstant()))
                .build();

        CreatePreauthenticatedRequestRequest request = CreatePreauthenticatedRequestRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .createPreauthenticatedRequestDetails(details)
                .build();

        CreatePreauthenticatedRequestResponse response = client.createPreauthenticatedRequest(request);

        PreauthenticatedRequest par = response.getPreauthenticatedRequest();

        return "https://objectstorage." + region + ".oraclecloud.com"
                + par.getAccessUri();
    }
}
