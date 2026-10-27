package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.parser.normalize.DocumentMarkdownNormalizer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIngestContentPreparerTest {

    @Test
    void writesNormalizedMarkdownForHtmlUpload() {
        KnowledgeObjectStore store = new InMemoryStore();
        DocumentMarkdownNormalizer normalizer = DocumentMarkdownNormalizer.create(true);
        KnowledgeDocument document = KnowledgeDocument.create(1L, null, "policy.html", "text/html",
                DocumentVisibility.TENANT, "key-1");
        byte[] html = "<h1>制度</h1><p>内容</p>".getBytes(StandardCharsets.UTF_8);

        var prepared = KnowledgeIngestContentPreparer.prepare(document, html, store, normalizer);

        assertThat(prepared.normalizedMarkdownKey()).endsWith("normalized/content.md");
        assertThat(prepared.fileName()).isEqualTo("content.md");
        assertThat(prepared.contentType()).isEqualTo("text/markdown");
        assertThat(new String(store.read(prepared.normalizedMarkdownKey()), StandardCharsets.UTF_8))
                .contains("制度");
    }

    private static final class InMemoryStore implements KnowledgeObjectStore {
        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public void put(String key, byte[] content, String contentType) {
            objects.put(key, content);
        }

        @Override
        public byte[] read(String key) {
            return objects.get(key);
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }
    }
}
