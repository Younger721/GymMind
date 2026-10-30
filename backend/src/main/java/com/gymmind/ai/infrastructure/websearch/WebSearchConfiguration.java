package com.gymmind.ai.infrastructure.websearch;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 启用联网搜索工具配置绑定 */
@Configuration
@EnableConfigurationProperties(WebSearchProperties.class)
public class WebSearchConfiguration {
}
