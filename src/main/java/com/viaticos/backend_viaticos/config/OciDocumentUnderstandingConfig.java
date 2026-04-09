package com.viaticos.backend_viaticos.config;

import com.oracle.bmc.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oracle.bmc.aidocument.AIServiceDocumentClient;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

@Configuration
public class OciDocumentUnderstandingConfig {

    @Value("${oci.region}")
    private String region;

    @Bean
    public AIServiceDocumentClient aiServiceDocumentClient(BasicAuthenticationDetailsProvider provider) {
        AIServiceDocumentClient client = AIServiceDocumentClient.builder().build(provider);
        client.setRegion(Region.fromRegionId(region));
        return client;
    }
}
