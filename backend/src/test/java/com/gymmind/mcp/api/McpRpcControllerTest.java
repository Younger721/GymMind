package com.gymmind.mcp.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.mcp.application.McpTool;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** MCP JSON-RPC 端点单测：握手/清单/调用/错误路径/通知报文 */
class McpRpcControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CurrentActor ACTOR = new CurrentActor(
            1L, 5L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:read", "search:read"), 0L, "token-1");

    private McpToolService tools;
    private McpRpcController controller;

    @BeforeEach
    void setUp() {
        tools = Mockito.mock(McpToolService.class);
        CurrentActorProvider actors = Mockito.mock(CurrentActorProvider.class);
        when(actors.requireCurrent()).thenReturn(ACTOR);
        when(tools.list(ACTOR)).thenReturn(List.of(
                new McpTool("knowledge_search", "检索知识库", "knowledge:read",
                        "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"required\":[\"query\"]}"),
                new McpTool("search_articles", "检索文章", "search:read", null)));
        controller = new McpRpcController(tools, actors);
    }

    @Test
    void initialize握手回协议版本与能力() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-03-26\"}}");
        ResponseEntity<JsonNode> response = controller.rpc(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = response.getBody();
        assertEquals(1, body.path("id").asInt());
        assertEquals("2.0", body.path("jsonrpc").asText());
        assertEquals("2025-03-26", body.path("result").path("protocolVersion").asText());
        assertFalse(body.path("result").path("capabilities").path("tools").isMissingNode());
        assertEquals("gymmind", body.path("result").path("serverInfo").path("name").asText());
    }

    @Test
    void 初始化通知返回202空体() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        ResponseEntity<JsonNode> response = controller.rpc(request);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void 工具清单带Schema且回显id() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/list\"}");
        ResponseEntity<JsonNode> response = controller.rpc(request);
        JsonNode toolsNode = response.getBody().path("result").path("tools");
        assertEquals(2, toolsNode.size());
        assertEquals("knowledge_search", toolsNode.get(0).path("name").asText());
        assertEquals("object", toolsNode.get(0).path("inputSchema").path("type").asText());
        // schema 缺省的工具回退默认 query schema
        assertEquals("query", toolsNode.get(1).path("inputSchema").path("properties").fieldNames().next());
        assertEquals(7, response.getBody().path("id").asInt());
    }

    @Test
    void 工具调用优先取query参数() throws Exception {
        when(tools.invoke(ACTOR, "knowledge_search", "会员价目")).thenReturn(List.of("片段1"));
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"knowledge_search\",\"arguments\":{\"query\":\"会员价目\"}}}");
        ResponseEntity<JsonNode> response = controller.rpc(request);
        JsonNode result = response.getBody().path("result");
        assertFalse(result.path("isError").asBoolean());
        assertEquals("text", result.path("content").get(0).path("type").asText());
        assertTrue(result.path("content").get(0).path("text").asText().contains("片段1"));
    }

    @Test
    void 无query参数时按kv平铺() throws Exception {
        when(tools.invoke(eq(ACTOR), eq("import_article"), any())).thenReturn("导入成功");
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"import_article\",\"arguments\":{\"url\":\"https://a.com\",\"title\":\"标题\"}}}");
        controller.rpc(request);
        org.mockito.Mockito.verify(tools).invoke(ACTOR, "import_article", "url=https://a.com;title=标题");
    }

    @Test
    void 业务失败转为isError结果而非协议错误() throws Exception {
        when(tools.invoke(ACTOR, "knowledge_search", "x"))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "没有权限执行此操作"));
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"knowledge_search\",\"arguments\":{\"query\":\"x\"}}}");
        ResponseEntity<JsonNode> response = controller.rpc(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().path("result").path("isError").asBoolean());
        assertTrue(response.getBody().path("result").path("content").get(0).path("text").asText().contains("权限"));
    }

    @Test
    void 未知方法返回协议错误() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":12,\"method\":\"resources/list\"}");
        ResponseEntity<JsonNode> response = controller.rpc(request);
        assertEquals(-32601, response.getBody().path("error").path("code").asInt());
    }

    @Test
    void 缺method的报文按解析错误处理() {
        ResponseEntity<JsonNode> response = controller.rpc(MAPPER.createObjectNode());
        assertEquals(-32700, response.getBody().path("error").path("code").asInt());
    }

    @Test
    void 保活ping返回空结果() throws Exception {
        JsonNode request = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":13,\"method\":\"ping\"}");
        ResponseEntity<JsonNode> response = controller.rpc(request);
        assertTrue(response.getBody().path("result").isObject());
    }
}
