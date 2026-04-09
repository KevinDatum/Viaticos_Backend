package com.viaticos.backend_viaticos.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OciGenerativeAiConfig {

    @Value("${oci.region}")
    private String region;

    @Bean
    public GenerativeAiInferenceClient generativeAiInferenceClient(BasicAuthenticationDetailsProvider provider) {
        GenerativeAiInferenceClient client = GenerativeAiInferenceClient.builder().build(provider);
        client.setRegion(Region.fromRegionId(region));
        return client;
    }
}
