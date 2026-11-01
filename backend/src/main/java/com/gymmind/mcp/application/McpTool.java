package com.gymmind.mcp.application;

/**
 * 工具元数据：MCP 工具注册表的最小单元。
 * <p>一份工具表，两个出口：进程内（AgentChatService / REST {@code /api/v1/mcp}）
 * 与标准协议出口（{@code /api/v1/mcp/rpc}，JSON-RPC 2.0）。
 *
 * @param name            工具名（全局唯一）
 * @param description     给模型看的用途描述
 * @param permission      调用所需的权限码
 * @param inputSchemaJson 入参 JSON Schema（MCP tools/list 需要）；空时用默认 query schema
 */
public record McpTool(String name, String description, String permission, String inputSchemaJson) {

    /** 默认入参 schema：单个 query 字符串（与工具层 String input 约定对齐） */
    public static final String DEFAULT_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"required\":[\"query\"]}";

    /** 兼容既有三参构造：schema 缺省 */
    public McpTool(String name, String description, String permission) {
        this(name, description, permission, null);
    }

    /** 序列化 tools/list 时使用：空则回退默认 schema */
    public String schemaOrDefault() {
        return inputSchemaJson == null || inputSchemaJson.isBlank() ? DEFAULT_SCHEMA : inputSchemaJson;
    }
}
