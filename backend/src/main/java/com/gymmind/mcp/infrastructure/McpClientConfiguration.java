package com.gymmind.mcp.infrastructure;

import com.gymmind.mcp.application.DefaultMcpClientService;
import com.gymmind.mcp.application.McpClientProperties;
import com.gymmind.mcp.application.McpClientService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MCP 客户端装配：远程技能发现与调用 */
@Configuration
@EnableConfigurationProperties(McpClientProperties.class)
public class McpClientConfiguration {

    @Bean
    public McpClientService mcpClientService(McpClientProperties properties) {
        return new DefaultMcpClientService(properties);
    }
}
