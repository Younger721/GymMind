package com.gymmind.mcp.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP 客户端配置：agent 可连接的远程 MCP server 列表。
 * <p>与 {@code application.yml} 的 {@code gymmind.mcp.client.servers} 对齐；
 * 环境变量可用索引形式（{@code MCP_CLIENT_SERVERS_0_NAME} 等）逐项覆盖。
 */
@ConfigurationProperties(prefix = "gymmind.mcp.client")
public class McpClientProperties {

    /** 远程 MCP server 列表（空列表 = 未接入远程技能，agent 行为不变） */
    private List<ServerConfig> servers = new ArrayList<>();

    public List<ServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<ServerConfig> servers) {
        this.servers = servers == null ? new ArrayList<>() : servers;
    }

    /** 单个远程 MCP server 的连接配置 */
    public static class ServerConfig {
        /** server 名称（enabledTools 里 mcp:&lt;name&gt;:&lt;tool&gt; 的引用依据） */
        private String name;
        /** MCP 端点 URL（Streamable HTTP 形态的 JSON-RPC 端点） */
        private String url;
        /** 可选的 Bearer Token（服务间凭证或用户级 token） */
        private String token = "";
        /** 是否启用 */
        private boolean enabled = true;
        /** 单次调用超时（毫秒） */
        private long timeoutMs = 8000;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token == null ? "" : token.trim();
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = Math.max(1000, timeoutMs);
        }
    }
}
