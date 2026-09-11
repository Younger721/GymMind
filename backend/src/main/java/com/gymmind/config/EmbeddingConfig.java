package com.gymmind.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.openai.embedding.model}")
    private String model;

    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        return new OpenAiEmbeddingModel(openAiApi);
    }
}
