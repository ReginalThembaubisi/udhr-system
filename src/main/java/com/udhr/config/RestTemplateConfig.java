package com.udhr.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // A plain `new RestTemplate()` never times out, so a single hung external
    // dependency (Infermedica, OpenFDA, Open Food Facts, Africa's Talking) can
    // block a request thread indefinitely and eventually exhaust the server's
    // whole thread pool. Every outbound call in this app should share this bean.
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
