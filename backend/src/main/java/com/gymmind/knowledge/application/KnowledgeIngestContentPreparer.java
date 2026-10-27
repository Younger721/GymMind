package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.parser.normalize.DocumentMarkdownNormalizer;

/** 入库前：可选将 DOCX/HTML/PDF 转为 Markdown 并写入对象存储 */
public final class KnowledgeIngestContentPreparer {

    public record PreparedContent(byte[] bytes, String fileName, String contentType, String normalizedMarkdownKey) {
    }

    private KnowledgeIngestContentPreparer() {
    }

    public static PreparedContent prepare(KnowledgeDocument document, byte[] sourceBytes,
                                          KnowledgeObjectStore store,
                                          DocumentMarkdownNormalizer markdownNormalizer) {
        if (document == null || sourceBytes == null) {
            throw new IllegalArgumentException("document and bytes required");
        }
        if (markdownNormalizer == null || !markdownNormalizer.shouldConvert(document.fileName(), document.contentType())) {
            return new PreparedContent(sourceBytes, document.fileName(), document.contentType(), null);
        }
        byte[] markdown = markdownNormalizer.toMarkdownBytes(document.fileName(), document.contentType(), sourceBytes);
        String normalizedKey = KnowledgeNormalizedMarkdownPaths.resolveKey(document.objectKey());
        store.put(normalizedKey, markdown, "text/markdown; charset=utf-8");
        return new PreparedContent(markdown, "content.md", "text/markdown", normalizedKey);
    }
}
