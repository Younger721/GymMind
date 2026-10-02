package com.gymmind.ai.harness;

/** 安全的系统执行明细，不包含模型思考过程。 */
public record AiProgressDetail(
        String kind,
        String title,
        String documentId,
        String chunkId,
        String documentName,
        String excerpt,
        Integer rank) {
}
