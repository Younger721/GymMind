package com.gymmind.mcp.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.mcp.application.McpTool;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 标准协议出口（手写轻量子集）：JSON-RPC 2.0 over HTTP（Streamable HTTP 的非流式形态）。
 *
 * <p>支持的协议方法：
 * <ul>
 *   <li>{@code initialize} —— 握手，回协议版本与能力（tools）</li>
 *   <li>{@code notifications/initialized} —— 客户端通知，202 空响应</li>
 *   <li>{@code ping} —— 保活</li>
 *   <li>{@code tools/list} —— 列出当前 Actor 可见工具（含 JSON Schema）</li>
 *   <li>{@code tools/call} —— 调用工具；业务失败以 {@code isError:true} 结果返回（协议约定）</li>
 * </ul>
 *
 * <p>鉴权与租户：与 /api/v1 其余端点同一条 JWT 链路，token 内的租户与权限
 * 组装为 {@link CurrentActor}，多租户隔离原样生效——MCP 端点不构成隔离旁路。
 * 工具表与 REST 出口共用一份（见 {@link McpTool}），一处注册两处可见。
 */
@RestController
@RequestMapping("/api/v1/mcp")
@SecurityRequirement(name = "bearerAuth")
public class McpRpcController {

    private static final Logger log = LoggerFactory.getLogger(McpRpcController.class);
    /** 服务支持的协议版本：实现协议子集所需的最小能力集，与主流客户端（2025-03-26/2025-06-18）兼容 */
    private static final String PROTOCOL_VERSION = "2025-06-18";
    /** JSON-RPC 标准错误码 */
    private static final int CODE_PARSE_ERROR = -32700;
    private static final int CODE_METHOD_NOT_FOUND = -32601;
    private static final int CODE_INVALID_PARAMS = -32602;
    private static final int CODE_INTERNAL_ERROR = -32603;

    private final McpToolService tools;
    private final CurrentActorProvider actors;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpRpcController(McpToolService tools, CurrentActorProvider actors) {
        this.tools = tools;
        this.actors = actors;
    }

    @PostMapping(value = "/rpc", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> rpc(@RequestBody(required = false) JsonNode request) {
        if (request == null || !request.hasNonNull("method")) {
            // 解析错误按规范必须回 id:null 的错误报文，而不是当作通知吞掉
            return ResponseEntity.ok(error(request, CODE_PARSE_ERROR, "请求不是合法的 JSON-RPC 2.0 报文"));
        }
        String method = request.path("method").asText();
        boolean notification = request.path("id").isMissingNode() || request.path("id").isNull();
        try {
            switch (method) {
                case "initialize" -> {
                    // 协议约定：服务端从客户端支持的版本里选择自己支持的；这里直接采用客户端请求版本
                    String requested = request.path("params").path("protocolVersion").asText(PROTOCOL_VERSION);
                    ObjectNode result = mapper.createObjectNode();
                    result.put("protocolVersion", requested);
                    result.set("capabilities", mapper.createObjectNode().set("tools",
                            mapper.createObjectNode().put("listChanged", false)));
                    ObjectNode info = result.putObject("serverInfo");
                    info.put("name", "gymmind");
                    info.put("version", "0.0.1");
                    info.put("title", "GymMind 健身管理平台");
                    return respond(request, result);
                }
                case "notifications/initialized", "notifications/cancelled" -> {
                    // 通知报文无需响应体
                    return ResponseEntity.accepted().build();
                }
                case "ping" -> {
                    return respond(request, mapper.createObjectNode());
                }
                case "tools/list" -> {
                    return respond(request, listTools());
                }
                case "tools/call" -> {
                    return respond(request, callTool(request.path("params")));
                }
                default -> {
                    if (notification) {
                        return ResponseEntity.accepted().build();
                    }
                    return ResponseEntity.ok(error(request, CODE_METHOD_NOT_FOUND, "不支持的方法: " + method));
                }
            }
        } catch (BusinessException failure) {
            // 业务失败按协议约定转为带 isError 的工具结果，而非协议错误
            ObjectNode result = mapper.createObjectNode();
            result.set("content", textContent(failure.getMessage()));
            result.put("isError", true);
            return respond(request, result);
        } catch (RuntimeException failure) {
            log.warn("MCP 调用内部错误: method={}", method, failure);
            return ResponseEntity.ok(error(request, CODE_INTERNAL_ERROR, "服务内部错误"));
        }
    }

    /** tools/list：当前 Actor 有权限可见的工具清单（含入参 Schema） */
    private JsonNode listTools() {
        CurrentActor actor = actors.requireCurrent();
        List<McpTool> visible = tools.list(actor);
        ObjectNode result = mapper.createObjectNode();
        ArrayNode array = result.putArray("tools");
        for (McpTool tool : visible) {
            ObjectNode node = array.addObject();
            node.put("name", tool.name());
            node.put("description", tool.description());
            try {
                node.set("inputSchema", mapper.readTree(tool.schemaOrDefault()));
            } catch (Exception malformed) {
                // schema 兜底：解析失败时退回默认结构，不让单个坏配置拖垮整个清单
                node.set("inputSchema", mapper.createObjectNode().put("type", "object"));
            }
        }
        return result;
    }

    /** tools/call：params.name + params.arguments → 工具层 String input */
    private JsonNode callTool(JsonNode params) {
        String name = params.path("name").asText("");
        if (name.isBlank()) {
            throw new BusinessException(com.gymmind.shared.error.ErrorCode.VALIDATION_FAILED, "缺少工具名 name");
        }
        CurrentActor actor = actors.requireCurrent();
        String input = toToolInput(params.path("arguments"));
        Object outcome = tools.invoke(actor, name, input);
        ObjectNode result = mapper.createObjectNode();
        result.set("content", textContent(outcome == null ? "（空结果）" : String.valueOf(outcome)));
        result.put("isError", false);
        return result;
    }

    /**
     * MCP arguments(JSON 对象) → 工具层 String input：
     * 优先取 query 字段；否则按 k=v 平铺（与 import_article 的字段解析约定一致）。
     */
    private String toToolInput(JsonNode arguments) {
        if (arguments == null || !arguments.isObject() || arguments.isEmpty()) {
            return "";
        }
        if (arguments.hasNonNull("query") && !arguments.path("query").asText().isBlank()) {
            return arguments.path("query").asText();
        }
        Map<String, String> flattened = new LinkedHashMap<>();
        arguments.fields().forEachRemaining(field -> {
            JsonNode value = field.getValue();
            if (value.isValueNode()) {
                flattened.put(field.getKey(), value.asText());
            }
        });
        if (flattened.isEmpty()) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        flattened.forEach((key, value) -> {
            if (joined.length() > 0) {
                joined.append(';');
            }
            joined.append(key).append('=').append(value);
        });
        return joined.toString();
    }

    private ArrayNode textContent(String text) {
        ArrayNode content = mapper.createArrayNode();
        ObjectNode item = content.addObject();
        item.put("type", "text");
        item.put("text", text);
        return content;
    }

    /** 构造完整 JSON-RPC 错误响应：解析错误等场景 id 为 null（协议约定） */
    private ObjectNode error(JsonNode request, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (request != null && request.hasNonNull("id")) {
            response.set("id", request.path("id"));
        } else {
            response.putNull("id");
        }
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return response;
    }

    /** 组装成功响应；请求缺 id（通知）时返回 202 空体 */
    private ResponseEntity<JsonNode> respond(JsonNode request, JsonNode result) {
        if (request != null && request.hasNonNull("id")) {
            ObjectNode response = mapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("id", request.path("id"));
            response.set("result", result);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
