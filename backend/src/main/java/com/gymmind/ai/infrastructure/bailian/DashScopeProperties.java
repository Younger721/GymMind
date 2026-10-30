package com.gymmind.ai.infrastructure.bailian;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 百炼（DashScope）OpenAI 兼容接口配置；与 {@code application.yml} 及环境变量 / 根目录 {@code .env} 对齐。
 */
@ConfigurationProperties(prefix = "gymmind.ai.dashscope")
public class DashScopeProperties {

    public static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private String apiKey = "";
    private String baseUrl = DEFAULT_BASE_URL;
    private String chatModel = "qwen3.8-flash";
    private String embeddingModel = "qwen3.7-text-embedding-flash";
    private String rerankModel = "qwen3.7-text-rerank";
    /** OCR 专用模型（文档/表格/试卷/手写优化），与 qwen-vl-ocr 相比为现役推荐型号 */
    private String ocrModel = "qwen3.5-ocr";
    /** OCR 备用模型：主模型免费额度用尽/限流（403/429）时回退一次，两者免费额度池独立 */
    private String ocrFallbackModel = "qwen3.8-flash";

    public String apiKeyForChat() {
        return trimToEmpty(apiKey);
    }

    public String apiKeyForDashScopeOnly() {
        return trimToEmpty(apiKey);
    }

    public String baseUrlForDashScope() {
        return defaultIfBlank(trimToEmpty(baseUrl), DEFAULT_BASE_URL);
    }

    public String baseUrlForChat() {
        return baseUrlForDashScope();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = defaultIfBlank(trimToEmpty(chatModel), "qwen3.8-flash");
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = defaultIfBlank(trimToEmpty(embeddingModel), "qwen3.7-text-embedding-flash");
    }

    public String getRerankModel() {
        return rerankModel;
    }

    public void setRerankModel(String rerankModel) {
        this.rerankModel = rerankModel;
    }

    public String getOcrModel() {
        return ocrModel;
    }

    public void setOcrModel(String ocrModel) {
        this.ocrModel = defaultIfBlank(trimToEmpty(ocrModel), "qwen3.5-ocr");
    }

    public String getOcrFallbackModel() {
        return ocrFallbackModel;
    }

    public void setOcrFallbackModel(String ocrFallbackModel) {
        this.ocrFallbackModel = defaultIfBlank(trimToEmpty(ocrFallbackModel), "qwen3.8-flash");
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultIfBlank(String raw, String fallback) {
        return raw.isBlank() ? fallback : raw;
    }
}
