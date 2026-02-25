package com.viaticos.backend_viaticos.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OciObjectStorageConfig {

    @Value("${oci.region}")
    private String region;

    @Bean
    public ObjectStorageClient objectStorageClient() throws IOException {
        var provider = new ConfigFileAuthenticationDetailsProvider("VIATICOS");
        ObjectStorageClient client = ObjectStorageClient.builder().build(provider);
        client.setRegion(Region.fromRegionId(region));
        return client;
    }
}