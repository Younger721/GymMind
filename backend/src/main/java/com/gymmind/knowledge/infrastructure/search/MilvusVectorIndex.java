package com.gymmind.knowledge.infrastructure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "gymmind.search.vector-backend", havingValue = "milvus")
public class MilvusVectorIndex implements VectorIndexPort {
    private static final List<String> OUTPUT_FIELDS = List.of("id", "tenantId", "ownerUserId", "text");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RestClient client;
    private final String collection;

    public MilvusVectorIndex(RestClient.Builder builder) {
        client = builder.baseUrl(Optional.ofNullable(System.getenv("GYMMIND_MILVUS_URL"))
                .orElse("http://localhost:19530")).build();
        collection = Optional.ofNullable(System.getenv("GYMMIND_MILVUS_COLLECTION"))
                .orElse("gymmind_knowledge");
    }

    public static Map<String, Object> collectionBody(String name) {
        var fields = List.of(
                field("id", "VarChar", true, Map.of("max_length", "128")),
                field("tenantId", "Int64", false, Map.of()),
                field("ownerUserId", "Int64", false, Map.of()),
                field("text", "VarChar", false, Map.of("max_length", "65535")),
                field("vector", "FloatVector", false, Map.of("dim", "1024")));
        return Map.of("collectionName", name,
                "schema", Map.of("autoId", false, "enableDynamicField", false, "fields", fields),
                "indexParams", List.of(Map.of("fieldName", "vector", "indexName", "vector_index",
                        "indexType", "HNSW", "metricType", "COSINE",
                        "params", Map.of("M", 16, "efConstruction", 200))));
    }

    private static Map<String, Object> field(String name, String type, boolean primary, Map<String, String> params) {
        return Map.of("fieldName", name, "dataType", type, "isPrimary", primary, "elementTypeParams", params);
    }

    public static Map<String, Object> insertBody(String name, IndexedChunk chunk) {
        return Map.of("collectionName", name, "data", List.of(Map.of(
                "id", chunk.id(), "tenantId", chunk.tenantId(),
                "ownerUserId", chunk.ownerUserId() == null ? 0L : chunk.ownerUserId(),
                "text", chunk.text(), "vector", chunk.vector())));
    }

    public static Map<String, Object> searchBody(String name, float[] vector, long tenantId, long userId) {
        return searchBody(name, vector, tenantId,
                "tenantId == " + tenantId + " && (ownerUserId == 0 || ownerUserId == " + userId + ")");
    }

    private static Map<String, Object> searchBody(String name, float[] vector, long tenantId, String filter) {
        return Map.of("collectionName", name, "data", List.of(vector), "annsField", "vector", "limit", 30,
                "filter", filter, "outputFields", OUTPUT_FIELDS);
    }

    public static List<IndexedChunk> parseSearchResponse(String raw) {
        try {
            var rows = MAPPER.readTree(raw).path("data");
            var chunks = new ArrayList<IndexedChunk>();
            for (var row : rows) {
                long owner = row.path("ownerUserId").asLong();
                chunks.add(new IndexedChunk(row.path("id").asText(), row.path("tenantId").asLong(),
                        owner == 0 ? null : owner, row.path("text").asText(), null));
            }
            return chunks;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public static boolean collectionExists(String raw) {
        try { return MAPPER.readTree(raw).path("data").path("has").asBoolean(false); }
        catch (Exception ignored) { return false; }
    }

    @Override
    public void upsert(IndexedChunk chunk) {
        requireVector(chunk.vector());
        client.post().uri("/v2/vectordb/entities/insert").body(insertBody(collection, chunk))
                .retrieve().toBodilessEntity();
    }

    @Override
    public List<IndexedChunk> search(float[] vector, Long tenantId) {
        requireVector(vector);
        return search(searchBody(collection, vector, tenantId, "tenantId == " + tenantId));
    }

    @Override
    public List<IndexedChunk> search(float[] vector, Long tenantId, Long userId) {
        requireVector(vector);
        return userId == null ? search(vector, tenantId) : search(searchBody(collection, vector, tenantId, userId));
    }

    private List<IndexedChunk> search(Map<String, Object> body) {
        String raw = client.post().uri("/v2/vectordb/entities/search").body(body).retrieve().body(String.class);
        return raw == null ? List.of() : parseSearchResponse(raw);
    }

    @Override
    public void delete(String id, Long tenantId) {
        client.post().uri("/v2/vectordb/entities/delete").body(Map.of("collectionName", collection,
                        "filter", "tenantId == " + tenantId + " && id == '" + id.replace("'", "''") + "'"))
                .retrieve().toBodilessEntity();
    }

    private static void requireVector(float[] vector) {
        if (vector == null || vector.length != 1024) throw new IllegalArgumentException("vector dimension must be 1024");
    }
}
