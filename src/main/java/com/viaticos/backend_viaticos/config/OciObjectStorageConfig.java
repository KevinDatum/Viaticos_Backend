package com.viaticos.backend_viaticos.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OciObjectStorageConfig {

    @Value("${oci.region}")
    private String region;

    @Bean
    // 👇 CAMBIO AQUÍ: Agregamos "Client" al tipo de retorno
    public ObjectStorageClient objectStorageClient(BasicAuthenticationDetailsProvider provider) {
        ObjectStorageClient client = ObjectStorageClient.builder().build(provider);
        client.setRegion(Region.fromRegionId(region));
        return client;
    }
}