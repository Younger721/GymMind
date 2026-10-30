package com.gymmind.ocr.domain;

import com.gymmind.ai.application.ModelUsage;

import java.util.List;

/**
 * OCR 识别结果：契约可回灌（约束③）——Markdown 可直接转 {@code ContextSegment}
 * 或作为知识文档入库，抽离后相同 JSON 原样走网络。
 */
public record OcrResult(List<OcrPage> pages,
                        String markdown,
                        String provider,
                        String model,
                        String providerRequestId,
                        ModelUsage usage) {

    public OcrResult {
        if (pages == null) {
            pages = List.of();
        }
        if (markdown == null) {
            markdown = "";
        }
    }
}
