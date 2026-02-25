package com.viaticos.backend_viaticos.config;

import com.oracle.bmc.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oracle.bmc.aidocument.AIServiceDocumentClient;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;

@Configuration
public class OciDocumentUnderstandingConfig {

    @Value("${oci.region}")
    private String region;

    @Value("${oci.config.path}")
    private String configPath;

    @Value("${oci.config.profile}")
    private String configProfile;

    @Bean
    public AIServiceDocumentClient aiServiceDocumentClient() throws Exception {

        ConfigFileAuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(configPath, configProfile);

        AIServiceDocumentClient client = AIServiceDocumentClient.builder().build(provider);

        client.setRegion(Region.fromRegionId(region));

        return client;
    }
    
}
