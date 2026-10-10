package com.gymmind.knowledge.infrastructure.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class IndexGenerationServiceTest {
    @Test
    void publishesOnlyWhenBothIndexesAcceptEveryChunk() {
        var keyword = new RecordingKeyword();
        var vector = new RecordingVector();
        var service = new IndexGenerationService(keyword, vector);
        assertThat(service.publish(7L, "g2", List.of(new IndexedChunk("c", 7L, "text", new float[1024])))).isTrue();
        assertThat(service.currentGeneration(7L)).isEqualTo("g2");
        vector.fail = true;
        assertThat(service.publish(7L, "g3", List.of(new IndexedChunk("d", 7L, "new", new float[1024])))).isFalse();
        assertThat(service.currentGeneration(7L)).isEqualTo("g2");
        assertThat(keyword.deleted).contains("7:d");
    }

    @Test
    void deleteRemovesChunkFromBothIndexesOnlyForTenant() {
        var k = new RecordingKeyword();
        var v = new RecordingVector();
        var s = new IndexGenerationService(k, v);
        s.delete(7L, "c");
        assertThat(k.deleted).containsExactly("7:c");
        assertThat(v.deleted).containsExactly("7:c");
    }

    @Test
    void platformIndexTenantZeroCanPublishAndDelete() {
        var k = new RecordingKeyword();
        var v = new RecordingVector();
        var s = new IndexGenerationService(k, v);
        assertThat(s.publish(0L, "platform-generation",
                List.of(new IndexedChunk("platform-chunk", 0L, 42L, null, "text", new float[1024])))).isTrue();
        s.delete(0L, "platform-chunk");
        assertThat(k.deleted).containsExactly("0:platform-chunk");
        assertThat(v.deleted).containsExactly("0:platform-chunk");
    }

    @Test
    void deleteRemainsIdempotentWhenOneBackendCannotFindTheChunk() {
        var k = new RecordingKeyword();
        var v = new RecordingVector();
        var s = new IndexGenerationService(k, v);
        k.deleteFail = true;

        assertThatCode(() -> s.delete(7L, "c")).doesNotThrowAnyException();
        assertThat(v.deleted).containsExactly("7:c");
    }

    static class RecordingKeyword implements KeywordIndexPort {
        boolean fail;
        boolean deleteFail;
        java.util.List<String> deleted = new java.util.ArrayList<>();

        public void upsert(IndexedChunk c) {
            if (fail) {
                throw new IllegalStateException();
            }
        }

        public List<IndexedChunk> search(String q, Long t) {
            return List.of();
        }

        public void delete(String id, Long t) {
            if (deleteFail) {
                throw new IllegalStateException("missing");
            }
            deleted.add(t + ":" + id);
        }
    }

    static class RecordingVector implements VectorIndexPort {
        boolean fail;
        java.util.List<String> deleted = new java.util.ArrayList<>();

        public void upsert(IndexedChunk c) {
            if (fail) {
                throw new IllegalStateException();
            }
        }

        public List<IndexedChunk> search(float[] q, Long t) {
            return List.of();
        }

        public void delete(String id, Long t) {
            deleted.add(t + ":" + id);
        }
    }
}
