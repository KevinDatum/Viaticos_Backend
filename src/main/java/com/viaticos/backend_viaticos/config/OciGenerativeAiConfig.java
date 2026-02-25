package com.viaticos.backend_viaticos.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OciGenerativeAiConfig {

    @Value("${oci.region}")
    private String region;

    @Value("${oci.config.path}")
    private String configPath;

    @Value("${oci.config.profile}")
    private String configProfile;

    @Bean
    public GenerativeAiInferenceClient generativeAiInferenceClient() throws Exception {
        
        ConfigFileAuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(configPath, configProfile);

        GenerativeAiInferenceClient client = GenerativeAiInferenceClient.builder().build(provider);
        client.setRegion(Region.fromRegionId(region));
        
        return client;
    }
}
