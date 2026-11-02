package com.gymmind.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP 客户端实现：手写 JSON-RPC 2.0 子集（tools/list + tools/call），
 * 与 {@code McpRpcController} 的服务端子集同构——GymMind 既是 MCP server 也是 MCP client。
 * <p>调用失败不阻断 agent 对话：发现阶段跳过失效 server，调用阶段抛出由调用方兜底。
 */
public class DefaultMcpClientService implements McpClientService {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClientService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpClientProperties props;
    private final AtomicLong ids = new AtomicLong(1);

    public DefaultMcpClientService(McpClientProperties props) {
        this.props = props;
    }

    @Override
    public List<RemoteMcpTool> listRemoteTools() {
        List<RemoteMcpTool> result = new ArrayList<>();
        for (McpClientProperties.ServerConfig server : props.getServers()) {
            if (!server.isEnabled() || blank(server.getName()) || blank(server.getUrl())) {
                continue;
            }
            try {
                JsonNode response = exchange(server, "tools/list", MAPPER.createObjectNode());
                JsonNode toolsNode = response.path("result").path("tools");
                if (!toolsNode.isArray()) {
                    continue;
                }
                for (JsonNode tool : toolsNode) {
                    String name = tool.path("name").asText("");
                    if (name.isBlank()) {
                        continue;
                    }
                    result.add(new RemoteMcpTool(server.getName(), name,
                            qualifiedName(server.getName(), name),
                            tool.path("description").asText(""),
                            tool.path("inputSchema").toString()));
                }
            } catch (RuntimeException failure) {
                // 单个 server 失效不阻断其余技能的发现
                log.warn("MCP server 工具发现失败: server={}, 原因={}", server.getName(), failure.getMessage());
            }
        }
        return result;
    }

    @Override
    public String callTool(String server, String tool, String queryInput) {
        McpClientProperties.ServerConfig config = findServer(server);
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", tool);
        ObjectNode arguments = params.putObject("arguments");
        if (queryInput != null && !queryInput.isBlank()) {
            arguments.put("query", queryInput);
        }
        JsonNode response = exchange(config, "tools/call", params);
        JsonNode result = response.path("result");
        if (result.path("isError").asBoolean(false)) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    firstText(result.path("content"), "远程工具执行失败: " + server + "/" + tool));
        }
        String text = firstText(result.path("content"), "");
        if (text.isBlank()) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "远程工具返回空结果: " + server + "/" + tool);
        }
        return text;
    }

    /** 发送 JSON-RPC 请求并解析响应（非流式 Streamable HTTP 形态）；protected 便于测试桩替换 */
    protected JsonNode exchange(McpClientProperties.ServerConfig server, String method, JsonNode params) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("jsonrpc", "2.0");
        body.put("id", ids.getAndIncrement());
        body.put("method", method);
        body.set("params", params);
        RestClient.RequestHeadersSpec<?> spec = clientFor(server).post()
                .uri(server.getUrl())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body);
        if (!blank(server.getToken())) {
            spec = spec.header("Authorization", "Bearer " + server.getToken());
        }
        String raw = spec.retrieve().body(String.class);
        JsonNode response;
        try {
            response = MAPPER.readTree(raw);
        } catch (Exception malformed) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "MCP server 响应解析失败: " + server.getName());
        }
        if (response.has("error")) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "MCP server 协议错误: " + response.path("error").path("message").asText("未知"));
        }
        return response;
    }

    /** 按配置构建带超时的 RestClient（server 数量少，直接重建即可） */
    private RestClient clientFor(McpClientProperties.ServerConfig server) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofMillis(server.getTimeoutMs()));
        return RestClient.builder().requestFactory(factory).build();
    }

    private McpClientProperties.ServerConfig findServer(String name) {
        for (McpClientProperties.ServerConfig server : props.getServers()) {
            if (server.isEnabled() && name != null && name.equals(server.getName())) {
                return server;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "未配置或未启用的 MCP server: " + name);
    }

    /** 提取 content 数组中的全部 text 块并以换行连接 */
    private static String firstText(JsonNode content, String fallback) {
        if (content.isArray()) {
            StringBuilder joined = new StringBuilder();
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText())) {
                    String text = item.path("text").asText("");
                    if (!text.isBlank()) {
                        if (joined.length() > 0) {
                            joined.append('\n');
                        }
                        joined.append(text);
                    }
                }
            }
            return joined.toString();
        }
        return fallback;
    }

    /** agent 侧唯一标识：mcp:&lt;server&gt;:&lt;tool&gt; */
    static String qualifiedName(String server, String tool) {
        return "mcp:" + server + ":" + tool;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
