package com.gymmind.ai.infrastructure.websearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 联网搜索工具配置；与 {@code application.yml} 的 {@code gymmind.ai.web-search} 对齐。
 * <p>provider 当前支持：
 * <ul>
 *   <li>{@code tavily}：使用 GitHub 开源 LangChain4j 官方的 Tavily 搜索工具
 *       （langchain4j-web-search-engine-tavily），需在 tavily.com 免费注册 API Key（1000 次/月）</li>
 *   <li>{@code none}：关闭联网搜索工具，模型调用时返回「未启用」</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "gymmind.ai.web-search")
public class WebSearchProperties {

    public static final String PROVIDER_TAVILY = "tavily";
    public static final String PROVIDER_NONE = "none";

    /** 搜索后端提供方，默认 tavily */
    private String provider = PROVIDER_TAVILY;

    /** Tavily 搜索服务 API Key（tavily.com 免费注册，对应环境变量 AI_WEB_SEARCH_API_KEY） */
    private String apiKey = "";

    /** 联网搜索返回的结果条数上限 */
    private int maxResults = 5;

    /** 搜索/抓取请求整体超时（毫秒） */
    private long timeoutMs = 8000;

    /** 单个网页正文注入 AI 上下文的最大字符数，避免超长页面挤占 token */
    private int maxContentChars = 6000;

    /** 单个网页下载的最大字节数，防止超大文件拖垮内存 */
    private long maxDownloadBytes = 524288L;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider == null || provider.isBlank() ? PROVIDER_TAVILY : provider.trim().toLowerCase();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = Math.max(1, Math.min(maxResults, 10));
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = Math.max(1000, timeoutMs);
    }

    public int getMaxContentChars() {
        return maxContentChars;
    }

    public void setMaxContentChars(int maxContentChars) {
        this.maxContentChars = Math.max(500, Math.min(maxContentChars, 12000));
    }

    public long getMaxDownloadBytes() {
        return maxDownloadBytes;
    }

    public void setMaxDownloadBytes(long maxDownloadBytes) {
        this.maxDownloadBytes = Math.max(10240, maxDownloadBytes);
    }
}
