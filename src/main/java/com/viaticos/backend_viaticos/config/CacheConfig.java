package com.viaticos.backend_viaticos.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager("parWebpUrls");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.HOURS) // ⏳ dura 3 horas
                .maximumSize(5000)); // límite para que no se llene infinito

        return cacheManager;
    }
    
}
