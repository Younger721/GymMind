package com.gymmind.ocr.application;

import java.util.List;

/** 识别结果视图：Markdown + 分页文本 + 用量计量（契约可回灌，M3 直接转 ContextSegment） */
public record OcrResultView(Long uploadId,
                            String status,
                            String markdown,
                            List<String> pages,
                            String provider,
                            String model,
                            Integer inputTokens,
                            Integer outputTokens,
                            Integer totalTokens) {
}
