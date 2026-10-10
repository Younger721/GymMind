package com.gymmind.knowledge.infrastructure.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

@Component
@ConditionalOnProperty(name = "gymmind.search.vector-backend", havingValue = "milvus")
public class MilvusVectorIndex implements VectorIndexPort {
    private static final List<String> OUTPUT_FIELDS = List.of("id", "tenantId", "documentId", "ownerUserId", "knowledgeKey", "knowledgeVersion", "indexGeneration", "text");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RestClient client;
    private final String collection;
    private volatile boolean collectionReady;

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
                field("documentId", "Int64", false, Map.of()),
                field("ownerUserId", "Int64", false, Map.of()),
                field("knowledgeKey", "VarChar", false, Map.of("max_length", "128")),
                field("knowledgeVersion", "Int64", false, Map.of()),
                field("indexGeneration", "VarChar", false, Map.of("max_length", "128")),
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
                "documentId", chunk.documentId() == null ? 0L : chunk.documentId(),
                "ownerUserId", chunk.ownerUserId() == null ? 0L : chunk.ownerUserId(),
                "knowledgeKey", chunk.knowledgeKey() == null ? "" : chunk.knowledgeKey(), "knowledgeVersion", chunk.knowledgeVersion(),
                "indexGeneration", chunk.indexGeneration() == null ? "" : chunk.indexGeneration(),
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
                long documentId = row.path("documentId").asLong();
                chunks.add(new IndexedChunk(row.path("id").asText(), row.path("tenantId").asLong(),
                        documentId == 0 ? null : documentId,
                        owner == 0 ? null : owner, row.path("text").asText(), null,
                        row.path("knowledgeKey").asText(null), row.path("knowledgeVersion").asInt(1),
                        emptyToNull(row.path("indexGeneration").asText(null))));
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
        ensureCollection();
        writeEntities("/v2/vectordb/entities/upsert", insertBody(collection, chunk));
    }

    private void ensureCollection() {
        if (collectionReady) return;
        synchronized (this) {
            if (collectionReady) return;
            boolean exists = collectionExists(client.post().uri("/v2/vectordb/collections/has")
                    .body(Map.of("collectionName", collection)).retrieve().body(String.class));
            if (exists && !schemaCompatible()) {
                dropCollection();
                exists = false;
            }
            if (!exists) {
                createCollection();
            }
            collectionReady = true;
        }
    }

    private void createCollection() {
        requireSuccess(client.post().uri("/v2/vectordb/collections/create").body(collectionBody(collection))
                .retrieve().body(String.class), "create collection");
    }

    private void dropCollection() {
        requireSuccess(client.post().uri("/v2/vectordb/collections/drop")
                        .body(Map.of("collectionName", collection)).retrieve().body(String.class),
                "drop collection");
    }

    /** 旧集合缺少 knowledgeKey 等字段时 upsert 会静默失败，需重建 schema。 */
    private boolean schemaCompatible() {
        String raw = client.post().uri("/v2/vectordb/collections/describe")
                .body(Map.of("collectionName", collection)).retrieve().body(String.class);
        try {
            JsonNode fields = MAPPER.readTree(raw).path("data").path("fields");
            if (!fields.isArray()) {
                return false;
            }
            Set<String> names = StreamSupport.stream(fields.spliterator(), false)
                    .map(node -> node.path("name").asText(""))
                    .filter(name -> !name.isBlank())
                    .collect(java.util.stream.Collectors.toSet());
            return names.containsAll(List.of("knowledgeKey", "knowledgeVersion", "documentId", "indexGeneration", "vector"));
        } catch (Exception ex) {
            return false;
        }
    }

    private void writeEntities(String path, Map<String, Object> body) {
        requireSuccess(client.post().uri(path).body(body).retrieve().body(String.class), "write entities");
    }

    private void requireSuccess(String raw, String action) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Milvus " + action + " returned empty body");
        }
        try {
            JsonNode root = MAPPER.readTree(raw);
            if (root.path("code").asInt(-1) != 0) {
                throw new IllegalStateException("Milvus " + action + " failed: " + root.path("message").asText(raw));
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Milvus " + action + " returned invalid JSON", ex);
        }
    }

    @Override
    public List<IndexedChunk> search(float[] vector, Long tenantId) {
        requireVector(vector);
        ensureCollection();
        return search(searchBody(collection, vector, tenantId, "tenantId == " + tenantId));
    }

    @Override
    public List<IndexedChunk> search(float[] vector, Long tenantId, Long userId) {
        requireVector(vector);
        ensureCollection();
        return userId == null ? search(vector, tenantId) : search(searchBody(collection, vector, tenantId, userId));
    }

    private List<IndexedChunk> search(Map<String, Object> body) {
        String raw = client.post().uri("/v2/vectordb/entities/search").body(body).retrieve().body(String.class);
        return raw == null ? List.of() : parseSearchResponse(raw);
    }

    @Override
    public void delete(String id, Long tenantId) {
        ensureCollection();
        client.post().uri("/v2/vectordb/entities/delete").body(Map.of("collectionName", collection,
                        "filter", "tenantId == " + tenantId + " && id == '" + id.replace("'", "''") + "'"))
                .retrieve().toBodilessEntity();
    }

    public void deleteDocument(Long tenantId, Long documentId) {
        ensureCollection();
        client.post().uri("/v2/vectordb/entities/delete").body(Map.of("collectionName", collection,
                        "filter", "tenantId == " + tenantId + " && documentId == " + documentId))
                .retrieve().toBodilessEntity();
    }

    @Override
    public void deleteStaleIndexGenerations(Long tenantId, Long documentId, String keepGeneration) {
        ensureCollection();
        String escaped = keepGeneration.replace("'", "''");
        String filter = "tenantId == " + tenantId + " && documentId == " + documentId
                + " && indexGeneration != '" + escaped + "'";
        client.post().uri("/v2/vectordb/entities/delete").body(Map.of("collectionName", collection, "filter", filter))
                .retrieve().toBodilessEntity();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void requireVector(float[] vector) {
        if (vector == null || vector.length != 1024) throw new IllegalArgumentException("vector dimension must be 1024");
    }
}
