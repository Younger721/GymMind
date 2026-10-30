package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.ai.infrastructure.websearch.WebSearchProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 网页读取工具测试：参数校验、SSRF 防护与正文提取 */
class AiWebFetchCapabilityTest {

    private final WebSearchProperties properties = new WebSearchProperties();

    private static ToolResult execute(AiWebFetchCapability capability, String url) {
        ObjectNode args = JsonNodeFactory.instance.objectNode();
        if (url != null) args.put("url", url);
        return capability.execute(null, args, new ToolExecutionContext(null, "问题", ignored -> { }));
    }

    @Test
    void 内网地址被拦截() {
        AiWebFetchCapability capability = new AiWebFetchCapability(properties);
        assertThat(AiWebFetchCapability.isBlockedUrl("http://localhost:8080/admin")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://127.0.0.1/x")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://10.0.0.5/x")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://172.16.0.1/x")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://192.168.1.1/x")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://169.254.169.254/meta")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://100.64.0.1/x")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://[::1]/x")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("file:///etc/passwd")).isTrue();
        assertThat(execute(capability, "http://192.168.1.1/secret").success()).isFalse();
    }

    @Test
    void 公网地址放行且非法地址拦截() {
        // 公网 IP 字面量不触发 DNS，测试稳定
        assertThat(AiWebFetchCapability.isBlockedUrl("https://1.1.1.1/dns-query")).isFalse();
        assertThat(AiWebFetchCapability.isBlockedUrl("http://8.8.8.8/")).isFalse();
        assertThat(AiWebFetchCapability.isBlockedUrl("ftp://1.1.1.1/")).isTrue();
        assertThat(AiWebFetchCapability.isBlockedUrl("not a url")).isTrue();
    }

    @Test
    void 正常抓取注入工具上下文() {
        AiWebFetchCapability capability = new AiWebFetchCapability(
                (url, timeoutMs, maxBytes) -> "<html><head><script>var a=1;</script></head>"
                        + "<body><h1>训练指南</h1><p>深蹲要点与常见错误。</p></body></html>",
                properties);
        ToolResult result = execute(capability, "https://1.1.1.1/guide");
        assertThat(result.success()).isTrue();
        assertThat(result.context()).hasSize(1);
        ContextSegment segment = result.context().get(0);
        assertThat(segment.documentId()).isEqualTo("TOOL_RESULT");
        assertThat(segment.text()).contains("训练指南").contains("深蹲要点").doesNotContain("var a=1");
    }

    @Test
    void 抓取异常转为工具错误() {
        AiWebFetchCapability capability = new AiWebFetchCapability(
                (url, timeoutMs, maxBytes) -> { throw new IllegalStateException("网页返回 HTTP 404"); },
                properties);
        ToolResult result = execute(capability, "https://1.1.1.1/missing");
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("404");
    }

    @Test
    void 缺少url参数返回错误() {
        AiWebFetchCapability capability = new AiWebFetchCapability(properties);
        assertThat(execute(capability, null).success()).isFalse();
    }

    @Test
    void 正文超长时截断() {
        properties.setMaxContentChars(500);
        AiWebFetchCapability capability = new AiWebFetchCapability(
                (url, timeoutMs, maxBytes) -> "正".repeat(2000), properties);
        ToolResult result = execute(capability, "https://1.1.1.1/long");
        assertThat(result.success()).isTrue();
        assertThat(result.context().get(0).text()).contains("已截断");
    }

    @Test
    void 描述符与参数Schema() {
        AiWebFetchCapability capability = new AiWebFetchCapability(properties);
        assertThat(capability.descriptor().id()).isEqualTo("web_fetch");
        assertThat(capability.descriptor().type()).isEqualTo("READ");
        assertThat(capability.descriptor().parameterSchema().path("required").get(0).asText()).isEqualTo("url");
    }
}
