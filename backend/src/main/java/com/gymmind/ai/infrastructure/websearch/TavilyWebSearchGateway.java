package com.gymmind.ai.infrastructure.websearch;

import com.gymmind.ai.application.WebSearchGateway;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * 基于 GitHub 开源项目 LangChain4j 官方模块 langchain4j-web-search-engine-tavily 的联网搜索网关。
 * <p>该模块提供了写好的 Tavily 搜索工具类 {@link TavilyWebSearchEngine}，本项目直接复用，
 * 只做一层「结果整理为模型上下文文本」的适配。
 */
@Component
@ConditionalOnProperty(prefix = "gymmind.ai.web-search", name = "provider",
        havingValue = WebSearchProperties.PROVIDER_TAVILY, matchIfMissing = true)
public class TavilyWebSearchGateway implements WebSearchGateway {

    /** 单条结果注入上下文的最大字符数，防止单条摘要过长 */
    private static final int MAX_ITEM_CHARS = 400;

    private final WebSearchProperties properties;

    /** 搜索引擎实现；懒构建，避免未配置 Key 时启动失败 */
    private volatile WebSearchEngine engine;

    @Autowired
    public TavilyWebSearchGateway(WebSearchProperties properties) {
        this(properties, null);
    }

    /** 测试用构造：直接注入搜索引擎实现，绕过真实网络 */
    TavilyWebSearchGateway(WebSearchProperties properties, WebSearchEngine engine) {
        this.properties = properties;
        this.engine = engine;
    }

    @Override
    public boolean available() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public String search(String query, int maxResults) {
        if (!available()) {
            throw new IllegalStateException("联网搜索未配置 API Key（环境变量 AI_WEB_SEARCH_API_KEY，tavily.com 免费注册）");
        }
        WebSearchResults results = engine().search(WebSearchRequest.builder()
                .searchTerms(query)
                .maxResults(maxResults)
                .build());
        return format(query, results);
    }

    /** 懒构建 Tavily 搜索引擎（LangChain4j 官方写好的工具类） */
    private WebSearchEngine engine() {
        if (engine == null) {
            synchronized (this) {
                if (engine == null) {
                    engine = TavilyWebSearchEngine.builder()
                            .apiKey(properties.getApiKey())
                            .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                            .build();
                }
            }
        }
        return engine;
    }

    /** 把 LangChain4j 搜索结果整理为带来源链接的纯文本，便于注入模型上下文 */
    static String format(String query, WebSearchResults results) {
        List<WebSearchOrganicResult> items = results == null ? null : results.results();
        if (items == null || items.isEmpty()) {
            return "未搜索到相关结果";
        }
        StringBuilder text = new StringBuilder("【联网搜索结果】").append(query).append('\n');
        int index = 1;
        for (WebSearchOrganicResult item : items) {
            String summary = item.content() == null || item.content().isBlank()
                    ? item.snippet() : item.content();
            text.append(index++).append(". ").append(nullSafe(item.title())).append('\n')
                    .append(truncate(nullSafe(summary))).append('\n')
                    .append("来源：").append(item.url() == null ? "未知" : item.url()).append("\n\n");
        }
        return text.toString().trim();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String truncate(String value) {
        return value.length() <= MAX_ITEM_CHARS ? value : value.substring(0, MAX_ITEM_CHARS) + "…";
    }
}
