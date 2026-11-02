package com.gymmind.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 远程 MCP server 暴露的工具（MCP 客户端视角）。
 * {@code qualifiedName} 是 agent 侧的唯一标识：{@code mcp:<server>:<tool>}，
 * 直接写进 {@code TenantAgent.enabledTools} 即可为 agent 启用该技能。
 */
public record RemoteMcpTool(String server, String name, String qualifiedName,
                            String description, String inputSchemaJson) {
}
