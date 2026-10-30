package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.application.WebSearchGateway;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.ai.infrastructure.websearch.WebSearchProperties;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 联网搜索 function call 工具：模型可自主决定调用，搜索互联网公开信息补充回答 */
@Component
public class AiWebSearchCapability implements AiCapability {

    /** 工具注册 ID，前端能力列表与配置开关均使用该 ID */
    public static final String ID = "web_search";

    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            ID,
            "联网搜索",
            "搜索互联网公开信息。适用于：时效性问题（最新版本、近期新闻）、外部行业资讯、"
                    + "健身知识需要最新佐证等场景。用户消息中包含 URL 网页链接时不要用本工具，"
                    + "改用 web_fetch 读取该网页。参数 query 为简洁的搜索关键词。",
            "READ",
            "",
            false,
            Set.of(),
            schema());

    private final ObjectProvider<WebSearchGateway> gateways;
    private final WebSearchProperties properties;

    public AiWebSearchCapability(ObjectProvider<WebSearchGateway> gateways, WebSearchProperties properties) {
        this.gateways = gateways;
        this.properties = properties;
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        String query = arguments == null ? "" : arguments.path("query").asText("").trim();
        if (query.isEmpty()) {
            return ToolResult.error("缺少搜索关键词 query");
        }
        WebSearchGateway gateway = gateways == null ? null : gateways.getIfUnique();
        if (gateway == null) {
            return ToolResult.error("联网搜索未启用（当前 provider 不提供搜索实现）");
        }
        if (!gateway.available()) {
            return ToolResult.error("联网搜索暂不可用：尚未配置搜索服务 API Key");
        }
        try {
            String result = gateway.search(query, properties.getMaxResults());
            return ToolResult.context("web-search", "【联网搜索】" + query + "\n" + result);
        } catch (RuntimeException failure) {
            return ToolResult.error("联网搜索暂不可用："
                    + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()));
        }
    }

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("query").put("type", "string")
                .put("description", "搜索关键词，例如「2025 高血压运动指南」「卧推 常见错误」");
        schema.putArray("required").add("query");
        return schema;
    }
}
