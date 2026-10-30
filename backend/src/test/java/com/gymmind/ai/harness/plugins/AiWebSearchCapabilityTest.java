package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.application.WebSearchGateway;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.ai.infrastructure.websearch.WebSearchProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 联网搜索工具测试：参数校验、网关缺失与结果注入 */
class AiWebSearchCapabilityTest {

    private final WebSearchProperties properties = new WebSearchProperties();

    @SuppressWarnings("unchecked")
    private static ObjectProvider<WebSearchGateway> providerOf(WebSearchGateway gateway) {
        ObjectProvider<WebSearchGateway> provider = mock(ObjectProvider.class);
        when(provider.getIfUnique()).thenReturn(gateway);
        return provider;
    }

    private static ToolResult execute(AiWebSearchCapability capability, String query) {
        ObjectNode args = JsonNodeFactory.instance.objectNode();
        if (query != null) args.put("query", query);
        return capability.execute(null, args, new ToolExecutionContext(null, "问题", ignored -> { }));
    }

    @Test
    void 正常搜索注入工具上下文() {
        WebSearchGateway gateway = mock(WebSearchGateway.class);
        when(gateway.available()).thenReturn(true);
        when(gateway.search(anyString(), anyInt())).thenReturn("【联网搜索结果】...\n来源：https://example.com");
        AiWebSearchCapability capability = new AiWebSearchCapability(providerOf(gateway), properties);
        ToolResult result = execute(capability, "最新健身趋势");
        assertThat(result.success()).isTrue();
        assertThat(result.context().get(0).documentId()).isEqualTo("TOOL_RESULT");
        assertThat(result.context().get(0).text()).contains("来源：https://example.com");
    }

    @Test
    void 网关缺失时返回未启用() {
        AiWebSearchCapability capability = new AiWebSearchCapability(providerOf(null), properties);
        assertThat(execute(capability, "任意关键词").success()).isFalse();
    }

    @Test
    void 未配置Key时返回不可用() {
        WebSearchGateway gateway = mock(WebSearchGateway.class);
        when(gateway.available()).thenReturn(false);
        AiWebSearchCapability capability = new AiWebSearchCapability(providerOf(gateway), properties);
        ToolResult result = execute(capability, "任意关键词");
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("API Key");
    }

    @Test
    void 搜索异常转为工具错误() {
        WebSearchGateway gateway = mock(WebSearchGateway.class);
        when(gateway.available()).thenReturn(true);
        when(gateway.search(anyString(), anyInt())).thenThrow(new IllegalStateException("联网搜索服务暂不可用"));
        AiWebSearchCapability capability = new AiWebSearchCapability(providerOf(gateway), properties);
        assertThat(execute(capability, "任意关键词").success()).isFalse();
    }

    @Test
    void 缺少query参数返回错误() {
        AiWebSearchCapability capability = new AiWebSearchCapability(providerOf(null), properties);
        assertThat(execute(capability, null).success()).isFalse();
    }

    @Test
    void 描述符与参数Schema() {
        AiWebSearchCapability capability = new AiWebSearchCapability(providerOf(null), properties);
        assertThat(capability.descriptor().id()).isEqualTo("web_search");
        assertThat(capability.descriptor().type()).isEqualTo("READ");
        assertThat(capability.descriptor().parameterSchema().path("required").get(0).asText()).isEqualTo("query");
    }
}
