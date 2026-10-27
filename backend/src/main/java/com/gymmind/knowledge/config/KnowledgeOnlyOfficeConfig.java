package com.gymmind.knowledge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KnowledgeOnlyOfficeProperties.class)
public class KnowledgeOnlyOfficeConfig {
}
