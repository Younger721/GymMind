package com.gymmind.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.gymmind.entity.KnowledgeChunk;
import com.gymmind.repository.KnowledgeChunkRepository;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStorageService {

    private final MilvusServiceClient milvusClient;
    private final ElasticsearchClient elasticsearchClient;
    private final KnowledgeChunkRepository chunkRepository;

    @Value("${milvus.collection.knowledge}")
    private String milvusCollection;

    @Value("${elasticsearch.index.knowledge}")
    private String esIndex;

    @Value("${milvus.vector.dimension}")
    private int vectorDimension;

    private boolean milvusInitialized = false;
    private boolean esInitialized = false;

    public void initializeMilvusCollection() {
        if (milvusInitialized) {
            return;
        }

        try {
            // Check if collection exists
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(milvusCollection)
                            .build()
            );

            if (hasCollection.getData()) {
                log.info("Milvus collection already exists: {}", milvusCollection);
                milvusInitialized = true;
                return;
            }

            // Create collection
            List<FieldType> fields = new ArrayList<>();

            fields.add(FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(true)
                    .build());

            fields.add(FieldType.newBuilder()
                    .withName("user_id")
                    .withDataType(DataType.Int64)
                    .build());

            fields.add(FieldType.newBuilder()
                    .withName("document_id")
                    .withDataType(DataType.Int64)
                    .build());

            fields.add(FieldType.newBuilder()
                    .withName("chunk_index")
                    .withDataType(DataType.Int64)
                    .build());

            fields.add(FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(vectorDimension)
                    .build());

            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(milvusCollection)
                    .withSchema(CollectionSchemaParam.newBuilder()
                            .withFields(fields)
                            .build())
                    .build();

            R<RpcStatus> createResult = milvusClient.createCollection(createCollectionParam);

            if (createResult.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Failed to create Milvus collection: " + createResult.getMessage());
            }

            log.info("Created Milvus collection: {}", milvusCollection);

            // Create index
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(milvusCollection)
                    .withFieldName("embedding")
                    .withIndexType(io.milvus.param.IndexType.HNSW)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withExtraParam("{\"M\":16,\"efConstruction\":256}")
                    .build();

            R<RpcStatus> indexResult = milvusClient.createIndex(indexParam);

            if (indexResult.getStatus() != R.Status.Success.getCode()) {
                log.warn("Failed to create index: {}", indexResult.getMessage());
            } else {
                log.info("Created HNSW index on embedding field");
            }

            // Load collection
            milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(milvusCollection)
                            .build()
            );

            milvusInitialized = true;
            log.info("Milvus collection initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize Milvus collection", e);
            throw new RuntimeException("Failed to initialize Milvus collection", e);
        }
    }

    public void initializeElasticsearchIndex() {
        if (esInitialized) {
            return;
        }

        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(esIndex)).value();

            if (exists) {
                log.info("Elasticsearch index already exists: {}", esIndex);
                esInitialized = true;
                return;
            }

            // Create index with mappings
            elasticsearchClient.indices().create(c -> c
                    .index(esIndex)
                    .mappings(m -> m
                            .properties("user_id", p -> p.long_(l -> l))
                            .properties("document_id", p -> p.long_(l -> l))
                            .properties("document_name", p -> p.text(t -> t.analyzer("smartcn")))
                            .properties("chunk_index", p -> p.integer(i -> i))
                            .properties("chunk_text", p -> p.text(t -> t.analyzer("smartcn")))
                            .properties("category", p -> p.keyword(k -> k))
                            .properties("source_type", p -> p.keyword(k -> k))
                            .properties("source_url", p -> p.keyword(k -> k))
                            .properties("created_at", p -> p.date(d -> d))
                    )
            );

            log.info("Created Elasticsearch index: {}", esIndex);
            esInitialized = true;

        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch index", e);
            throw new RuntimeException("Failed to initialize Elasticsearch index", e);
        }
    }

    @Transactional
    public void storeChunk(Long userId, Long documentId, String documentName,
                          String category, String sourceType, String sourceUrl,
                          int chunkIndex, String chunkText, float[] embedding) {

        // Initialize if needed
        initializeMilvusCollection();
        initializeElasticsearchIndex();

        // Store in Milvus
        String milvusId = storeMilvusVector(userId, documentId, chunkIndex, embedding);

        // Store in Elasticsearch
        String esId = storeElasticsearchDocument(userId, documentId, documentName, category,
                sourceType, sourceUrl, chunkIndex, chunkText);

        // Store chunk metadata in MySQL
        KnowledgeChunk chunk = KnowledgeChunk.builder()
                .userId(userId)
                .documentId(documentId)
                .chunkIndex(chunkIndex)
                .chunkText(chunkText)
                .category(category)
                .sourceType(sourceType)
                .sourceUrl(sourceUrl)
                .documentName(documentName)
                .milvusId(milvusId)
                .esId(esId)
                .build();

        chunkRepository.save(chunk);

        log.debug("Stored chunk {}/{} for document {}", chunkIndex + 1, documentId, documentId);
    }

    private String storeMilvusVector(Long userId, Long documentId, int chunkIndex, float[] embedding) {
        try {
            List<InsertParam.Field> fields = new ArrayList<>();

            fields.add(new InsertParam.Field("user_id", Collections.singletonList(userId)));
            fields.add(new InsertParam.Field("document_id", Collections.singletonList(documentId)));
            fields.add(new InsertParam.Field("chunk_index", Collections.singletonList((long) chunkIndex)));
            fields.add(new InsertParam.Field("embedding", Collections.singletonList(embedding)));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(milvusCollection)
                    .withFields(fields)
                    .build();

            R<MutationResult> result = milvusClient.insert(insertParam);

            if (result.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Failed to insert vector: " + result.getMessage());
            }

            // Return Milvus auto-generated ID
            List<Long> ids = result.getData().getIDs().getIntId().getDataList();
            return ids.isEmpty() ? null : String.valueOf(ids.get(0));

        } catch (Exception e) {
            log.error("Failed to store vector in Milvus", e);
            throw new RuntimeException("Failed to store vector in Milvus", e);
        }
    }

    private String storeElasticsearchDocument(Long userId, Long documentId, String documentName,
                                           String category, String sourceType, String sourceUrl,
                                           int chunkIndex, String chunkText) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("user_id", userId);
            doc.put("document_id", documentId);
            doc.put("document_name", documentName);
            doc.put("chunk_index", chunkIndex);
            doc.put("chunk_text", chunkText);
            doc.put("category", category);
            doc.put("source_type", sourceType);
            doc.put("source_url", sourceUrl);
            doc.put("created_at", LocalDateTime.now().toString());

            String docId = String.format("%d_%d_%d", userId, documentId, chunkIndex);

            elasticsearchClient.index(IndexRequest.of(i -> i
                    .index(esIndex)
                    .id(docId)
                    .document(doc)
                    .refresh(Refresh.False)
            ));

            return docId;

        } catch (IOException e) {
            log.error("Failed to store document in Elasticsearch", e);
            throw new RuntimeException("Failed to store document in Elasticsearch", e);
        }
    }
}
