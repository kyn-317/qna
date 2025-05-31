package com.kyn.qna.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class GeminiClient {
    @Value("${google.api.key}")
    private String apiKey;

    @Bean 
    public Client aiClient() {
        return Client.builder()
        .apiKey(apiKey)
        .build();
    }
}
