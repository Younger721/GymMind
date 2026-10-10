package com.gymmind.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.parser.Chunk;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkArtifactServiceTest {

    @Test
    void writesAndReadsArtifactFromObjectStore() {
        InMemoryObjectStore store = new InMemoryObjectStore();
        KnowledgeChunkArtifactService service = new KnowledgeChunkArtifactService(store, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneOffset.UTC));
        KnowledgeDocument document = KnowledgeDocument.create(3L, null, "guide.md", "text/markdown",
                DocumentVisibility.TENANT, "uuid-segment");
        document.assignId(11L);
        List<Chunk> chunks = List.of(new Chunk("chunk-1", "11", 0, "EVAL-TEST-001"));

        String key = service.writeArtifact(document, chunks);

        assertThat(key).contains("/chunks/GENERAL-v1-");
        KnowledgeChunkArtifact read = service.readArtifact(key);
        assertThat(read.documentId()).isEqualTo(11L);
        assertThat(read.chunks()).hasSize(1);
        assertThat(read.chunks().get(0).text()).isEqualTo("EVAL-TEST-001");
    }

    @Test
    void resolveArtifactKeyUsesChunksDirectory() {
        String source = "tenant/3/knowledge/uuid-segment/guide.md";
        String key = KnowledgeChunkArtifactService.resolveArtifactKey(source, "GENERAL-v1-123");
        assertThat(key).isEqualTo("tenant/3/knowledge/uuid-segment/chunks/GENERAL-v1-123.json");
    }

    private static final class InMemoryObjectStore implements KnowledgeObjectStore {
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
