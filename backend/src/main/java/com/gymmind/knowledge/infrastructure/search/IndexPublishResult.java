package com.gymmind.knowledge.infrastructure.search;

/** 单次文档索引发布结果（generation 指针切换前由 Worker 写入 MySQL）。 */
public record IndexPublishResult(boolean success, String indexGeneration) {

    public static IndexPublishResult failed() {
        return new IndexPublishResult(false, null);
    }

    public static IndexPublishResult ok(String indexGeneration) {
        if (indexGeneration == null || indexGeneration.isBlank()) {
            throw new IllegalArgumentException("indexGeneration required");
        }
        return new IndexPublishResult(true, indexGeneration.trim());
    }
}
