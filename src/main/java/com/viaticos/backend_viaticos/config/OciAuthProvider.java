package com.viaticos.backend_viaticos.config;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.StringPrivateKeySupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OciAuthProvider {

    @Value("${oci.config.path:}")
    private String configPath;

    @Value("${oci.config.profile:}")
    private String configProfile;

    // Variables que vendrán de Docker
    @Value("${OCI_TENANCY_ID:}")
    private String tenancyId;

    @Value("${OCI_USER_ID:}")
    private String userId;

    @Value("${OCI_FINGERPRINT:}")
    private String fingerprint;

    // 🔥 AHORA RECIBIMOS EL TEXTO DE LA LLAVE, NO LA RUTA
    @Value("${OCI_PRIVATE_KEY_CONTENT:}")
    private String privateKeyContent;

    @Bean
    public BasicAuthenticationDetailsProvider authenticationDetailsProvider() throws IOException {
        if (tenancyId != null && !tenancyId.isEmpty()) {
            
            // Reemplazamos los saltos de línea literales por saltos reales (necesario por cómo Docker pasa las variables)
            String formattedKey = privateKeyContent.replace("\\n", "\n");

            return SimpleAuthenticationDetailsProvider.builder()
                    .tenantId(tenancyId)
                    .userId(userId)
                    .fingerprint(fingerprint)
                    .privateKeySupplier(new StringPrivateKeySupplier(formattedKey))
                    .build();
        } else {
            return new ConfigFileAuthenticationDetailsProvider(configPath, configProfile);
        }
    }
}
