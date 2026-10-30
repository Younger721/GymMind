package com.gymmind.ocr.domain;

/**
 * OCR 能力端口：与 {@code ChatModelGateway}/{@code EmbeddingGateway}/{@code RerankGateway} 同构，
 * 供应商可替换（默认 DashScope qwen3.5-ocr，未来可换本地模型或其他云服务）。
 * <p>设计约束：二进制只在本层出入，上层编排（服务/工具）只传 ID，为 AI 独立抽离保持契约稳定。
 */
public interface OcrGateway {

    /** 是否支持该内容类型（image/jpeg、image/png、image/webp、image/bmp） */
    boolean supports(String contentType);

    /** 识别图片，返回分页文本与规范化 Markdown */
    OcrResult recognize(OcrRequest request);
}
