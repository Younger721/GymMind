package com.gymmind.knowledge.infrastructure.parser.normalize;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** 判断上传格式并转换为 Markdown 字节（DOCX / HTML / PDF） */
public class DocumentMarkdownNormalizer {

    private final boolean enabled;

    public static DocumentMarkdownNormalizer create(boolean enabled) {
        return new DocumentMarkdownNormalizer(enabled);
    }

    private DocumentMarkdownNormalizer(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean shouldConvert(String fileName, String contentType) {
        if (!enabled) {
            return false;
        }
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("markdown") || name.endsWith(".md")) {
            return false;
        }
        if (type.contains("text/plain") || name.endsWith(".txt")) {
            return false;
        }
        return type.contains("wordprocessingml") || name.endsWith(".docx")
                || type.contains("text/html") || name.endsWith(".html") || name.endsWith(".htm")
                || type.contains("pdf") || name.endsWith(".pdf");
    }

    public byte[] toMarkdownBytes(String fileName, String contentType, byte[] bytes) {
        String markdown = toMarkdown(fileName, contentType, bytes);
        if (markdown.isBlank()) {
            throw new IllegalArgumentException("document conversion produced empty markdown");
        }
        return markdown.getBytes(StandardCharsets.UTF_8);
    }

    public String toMarkdown(String fileName, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("document is empty");
        }
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("wordprocessingml") || name.endsWith(".docx")) {
            return DocumentBlocksToMarkdown.render(DocxStructureParser.parse(bytes));
        }
        if (type.contains("text/html") || name.endsWith(".html") || name.endsWith(".htm")) {
            return HtmlMarkdownConverter.convert(bytes);
        }
        if (type.contains("pdf") || name.endsWith(".pdf")) {
            return PdfMarkdownConverter.convert(bytes);
        }
        throw new IllegalArgumentException("unsupported document type for markdown conversion");
    }
}
