package com.gymmind.ai.infrastructure.websearch;

import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tavily 搜索网关测试：结果格式化（不触发真实网络） */
class TavilyWebSearchGatewayTest {

    @Test
    void 搜索结果格式化为带来源的文本() {
        WebSearchResults results = new WebSearchResults(
                new WebSearchInformationResult(2L),
                List.of(new WebSearchOrganicResult("训练指南", URI.create("https://example.com/guide"),
                                null, "深蹲要点与常见错误。"),
                        new WebSearchOrganicResult("营养基础", URI.create("https://example.com/nutrition"),
                                "蛋白质摄入建议", null)));
        String text = TavilyWebSearchGateway.format("健身入门", results);
        assertThat(text).contains("【联网搜索结果】健身入门")
                .contains("训练指南")
                .contains("深蹲要点与常见错误。")
                .contains("蛋白质摄入建议")
                .contains("来源：https://example.com/guide")
                .contains("来源：https://example.com/nutrition");
    }

    @Test
    void 空结果返回未搜索到() {
        WebSearchResults empty = new WebSearchResults(new WebSearchInformationResult(0L), List.of());
        assertThat(TavilyWebSearchGateway.format("无结果查询", empty)).contains("未搜索到相关结果");
    }

    @Test
    void 未配置Key时不可用() {
        WebSearchProperties properties = new WebSearchProperties();
        TavilyWebSearchGateway gateway = new TavilyWebSearchGateway(properties);
        assertThat(gateway.available()).isFalse();
    }

    @Test
    void 配置Key后可用() {
        WebSearchProperties properties = new WebSearchProperties();
        properties.setApiKey("test-key");
        TavilyWebSearchGateway gateway = new TavilyWebSearchGateway(properties);
        assertThat(gateway.available()).isTrue();
    }
}
