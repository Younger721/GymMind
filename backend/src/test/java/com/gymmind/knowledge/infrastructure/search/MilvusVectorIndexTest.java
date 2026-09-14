package com.gymmind.knowledge.infrastructure.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MilvusVectorIndexTest {

    @Test
    void collectionUsesCosineHnswAndRequiredIsolationFields() {
        Map<String, Object> body = MilvusVectorIndex.collectionBody("gymmind_knowledge");

        assertThat(body).containsEntry("collectionName", "gymmind_knowledge");
        assertThat(body.get("indexParams").toString()).contains("COSINE", "HNSW", "vector");
        assertThat(body.get("schema")).isInstanceOf(Map.class);
        assertThat(body.get("schema").toString())
                .contains("id", "tenantId", "ownerUserId", "text", "vector", "1024");
    }

    @Test
    void insertCarriesTenantOwnerTextAndVector() {
        float[] vector = new float[1024];
        Map<String, Object> body = MilvusVectorIndex.insertBody(
                "gymmind_knowledge", new IndexedChunk("chunk-1", 7L, 11L, "private text", vector));

        assertThat(body).containsEntry("collectionName", "gymmind_knowledge");
        assertThat((List<?>) body.get("data")).singleElement().satisfies(row ->
                assertThat((Map<String, Object>) row)
                        .containsEntry("id", "chunk-1")
                        .containsEntry("tenantId", 7L)
                        .containsEntry("ownerUserId", 11L)
                        .containsEntry("text", "private text")
                        .containsEntry("vector", vector));
    }

    @Test
    void searchFiltersTenantAndPrivateOwner() {
        Map<String, Object> body = MilvusVectorIndex.searchBody(
                "gymmind_knowledge", new float[1024], 7L, 11L);

        assertThat(body).containsEntry("filter", "tenantId == 7 && (ownerUserId == 0 || ownerUserId == 11)");
        assertThat(body).containsEntry("limit", 30);
        assertThat(body.get("outputFields")).isEqualTo(List.of("id", "tenantId", "ownerUserId", "text"));
    }

    @Test
    void mapsMilvusSearchRowsBackToChunks() {
        String response = """
                {"code":0,"data":[{"id":"chunk-1","tenantId":7,"ownerUserId":11,"text":"private text","distance":0.91}]}
                """;

        assertThat(MilvusVectorIndex.parseSearchResponse(response)).containsExactly(
                new IndexedChunk("chunk-1", 7L, 11L, "private text", null));
    }

    @Test
    void readsCollectionExistenceResponse() {
        assertThat(MilvusVectorIndex.collectionExists("{\"code\":0,\"data\":{\"has\":true}}"))
                .isTrue();
        assertThat(MilvusVectorIndex.collectionExists("{\"code\":0,\"data\":{\"has\":false}}"))
                .isFalse();
    }
}
