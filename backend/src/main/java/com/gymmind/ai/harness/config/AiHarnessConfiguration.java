package com.gymmind.ai.harness.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiHarnessProperties.class)
public class AiHarnessConfiguration {
}
