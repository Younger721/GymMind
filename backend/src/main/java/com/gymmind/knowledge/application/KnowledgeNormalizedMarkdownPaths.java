package com.gymmind.knowledge.application;

/** 知识库规范化 Markdown 在对象存储中的路径规则 */
public final class KnowledgeNormalizedMarkdownPaths {

    public static final String NORMALIZED_FILE_NAME = "normalized/content.md";

    private KnowledgeNormalizedMarkdownPaths() {
    }

    /** 由原文件 objectKey 推导 normalized Markdown 的 objectKey */
    public static String resolveKey(String sourceObjectKey) {
        if (sourceObjectKey == null || sourceObjectKey.isBlank()) {
            throw new IllegalArgumentException("source object key required");
        }
        int slash = sourceObjectKey.lastIndexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("invalid source object key");
        }
        return sourceObjectKey.substring(0, slash + 1) + NORMALIZED_FILE_NAME;
    }
}
