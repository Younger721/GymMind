package com.gymmind.ai.application;

/**
 * 联网搜索网关：屏蔽底层搜索服务差异，返回已整理好的搜索结果文本。
 * <p>实现方需保证结果文本带来源链接，且不抛出受检异常（失败时抛 RuntimeException 由调用方转为工具错误）。
 */
public interface WebSearchGateway {

    /**
     * 执行联网搜索。
     *
     * @param query     搜索关键词
     * @param maxResults 期望的结果条数上限（搜索后端可能自行决定实际条数）
     * @return 带来源链接的搜索结果文本
     */
    String search(String query, int maxResults);

    /** 是否可用（例如 API Key 已配置） */
    default boolean available() {
        return true;
    }
}
