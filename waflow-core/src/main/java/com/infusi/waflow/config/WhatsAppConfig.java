package com.infusi.waflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {

    private final WhatsAppProperties properties;

    public WhatsAppConfig(WhatsAppProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WebClient whatsAppWebClient() {
        return WebClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
