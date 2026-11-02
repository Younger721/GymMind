package com.gymmind.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.mcp.application.McpClientProperties.ServerConfig;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MCP 客户端单测：技能发现映射、调用结果提取、失败降级与配置校验（HTTP 交换以测试桩替换） */
class DefaultMcpClientServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private McpClientProperties props;
    /** 记录每次 exchange 的 method 与 params，供断言 */
    private String lastMethod;
    private JsonNode lastParams;
    /** 测试桩返回的响应（按 method 键）与需要抛出的异常 */
    private final java.util.Map<String, JsonNode> responses = new java.util.HashMap<>();
    private final java.util.Map<String, RuntimeException> failures = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        props = new McpClientProperties();
        props.getServers().add(server("ops", "http://localhost:9300/mcp", "token-1", true));
        props.getServers().add(server("broken", "http://localhost:9400/mcp", "", true));
    }

    /** 构造被测服务：重写 exchange，按 method 返回预设响应或抛出预设异常 */
    private DefaultMcpClientService service() {
        return new DefaultMcpClientService(props) {
            @Override
            protected JsonNode exchange(ServerConfig server, String method, JsonNode params) {
                lastMethod = method;
                lastParams = params;
                RuntimeException failure = failures.get(method + "@" + server.getName());
                if (failure != null) {
                    throw failure;
                }
                JsonNode response = responses.get(method + "@" + server.getName());
                if (response == null) {
                    throw new IllegalStateException("测试桩缺少响应: " + method + "@" + server.getName());
                }
                return response;
            }
        };
    }

    @Test
    void 技能发现映射qualifiedName() throws Exception {
        responses.put("tools/list@ops", MAPPER.readTree("{\"result\":{\"tools\":["
                + "{\"name\":\"query_metrics\",\"description\":\"查询经营指标\",\"inputSchema\":{\"type\":\"object\"}}]}}"));
        List<RemoteMcpTool> tools = service().listRemoteTools();
        assertEquals(1, tools.size());
        assertEquals("mcp:ops:query_metrics", tools.get(0).qualifiedName());
        assertEquals("ops", tools.get(0).server());
        assertEquals("查询经营指标", tools.get(0).description());
    }

    @Test
    void 单个server失效不阻断其余技能发现() throws Exception {
        failures.put("tools/list@ops", new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE, "不可用"));
        responses.put("tools/list@broken", MAPPER.readTree("{\"result\":{\"tools\":[{\"name\":\"echo\"}]}}"));
        List<RemoteMcpTool> tools = service().listRemoteTools();
        assertEquals(1, tools.size());
        assertEquals("mcp:broken:echo", tools.get(0).qualifiedName());
    }

    @Test
    void 调用远程工具提取文本内容() throws Exception {
        responses.put("tools/call@ops", MAPPER.readTree("{\"result\":{\"content\":["
                + "{\"type\":\"text\",\"text\":\"本月营收 12 万\"},{\"type\":\"text\",\"text\":\"环比 +8%\"}],\"isError\":false}}"));
        String text = service().callTool("ops", "query_metrics", "本月营收");
        assertTrue(text.contains("本月营收 12 万"));
        assertTrue(text.contains("环比 +8%"));
        assertEquals("tools/call", lastMethod);
        assertEquals("query_metrics", lastParams.path("name").asText());
        assertEquals("本月营收", lastParams.path("arguments").path("query").asText());
    }

    @Test
    void 空输入不携带query参数() throws Exception {
        responses.put("tools/call@ops", MAPPER.readTree("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}}"));
        service().callTool("ops", "echo", "  ");
        assertTrue(lastParams.path("arguments").path("query").isMissingNode());
    }

    @Test
    void 远程工具报错转依赖不可用异常() throws Exception {
        responses.put("tools/call@ops", MAPPER.readTree(
                "{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"指标源离线\"}],\"isError\":true}}"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().callTool("ops", "query_metrics", "营收"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
        assertTrue(ex.getMessage().contains("指标源离线"));
    }

    @Test
    void 未配置的server校验失败() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().callTool("ghost", "tool", "x"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
    }

    @Test
    void 协议错误转依赖不可用() throws Exception {
        responses.put("tools/call@ops", MAPPER.readTree("{\"error\":{\"code\":-32601,\"message\":\"方法不存在\"}}"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().callTool("ops", "tool", "x"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void qualifiedName格式约定() {
        assertEquals("mcp:ops:query_metrics", DefaultMcpClientService.qualifiedName("ops", "query_metrics"));
    }

    private static ServerConfig server(String name, String url, String token, boolean enabled) {
        ServerConfig config = new ServerConfig();
        config.setName(name);
        config.setUrl(url);
        config.setToken(token);
        config.setEnabled(enabled);
        return config;
    }
}
