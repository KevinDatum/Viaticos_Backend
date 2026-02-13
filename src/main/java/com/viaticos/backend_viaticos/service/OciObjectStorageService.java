package com.viaticos.backend_viaticos.service;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OciObjectStorageService {

    private final ObjectStorageClient objectStorageClient;

    @Value("${oci.bucket.name}")
    private String bucketName;

    @Value("${oci.namespace}")
    private String namespace;

    /**
     * Sube un archivo al bucket.
     * Retorna el objectName para guardarlo en DB.
     */
    public String upload(InputStream inputStream, long contentLength, String contentType, String objectName) {

        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .contentLength(contentLength)
                .contentType(contentType)
                .putObjectBody(inputStream)
                .build();

        objectStorageClient.putObject(request);
        return objectName;
    }

    public void delete(String objectName) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .objectName(objectName)
                .build();

        objectStorageClient.deleteObject(request);
    }

    /**
     * Genera URL temporal usando PAR (Preauthenticated Request).
     */
    public String generateParUrl(String objectName, int minutesValid) {

        String parName = "par_" + UUID.randomUUID();

        CreatePreauthenticatedRequestDetails details = CreatePreauthenticatedRequestDetails.builder()
                .name(parName)
                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                .objectName(objectName)
                .timeExpires(java.util.Date.from(
                        OffsetDateTime.now().plusMinutes(minutesValid).toInstant()))
                .build();

        CreatePreauthenticatedRequestRequest request = CreatePreauthenticatedRequestRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .createPreauthenticatedRequestDetails(details)
                .build();

        CreatePreauthenticatedRequestResponse response = objectStorageClient.createPreauthenticatedRequest(request);

        PreauthenticatedRequest par = response.getPreauthenticatedRequest();

        // La URL completa base correcta:
        // https://objectstorage.us-chicago-1.oraclecloud.com + accessUri
        // pero a veces OCI devuelve dominio customer-oci listo.
        // Mejor devolver el full path manualmente usando el endpoint oficial:
        String accessUri = par.getAccessUri();

        return "https://objectstorage.us-chicago-1.oraclecloud.com" + accessUri;
    }
}