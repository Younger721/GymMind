package com.gymmind.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.gymmind.entity.KnowledgeImage;
import com.gymmind.repository.KnowledgeImageRepository;
import com.gymmind.security.SecurityUtils;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.MutationResult;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final KnowledgeImageRepository imageRepository;
    private final MinioService minioService;
    private final ImageEmbeddingService imageEmbeddingService;
    private final MilvusServiceClient milvusClient;
    private final ElasticsearchClient elasticsearchClient;
    private final WebSocketService webSocketService;

    @Value("${minio.bucket.images}")
    private String imageBucket;

    @Value("${milvus.collection.knowledge}")
    private String milvusCollection;

    @Value("${elasticsearch.index.knowledge}")
    private String esIndex;

    @Transactional
    public KnowledgeImage uploadImage(MultipartFile file, String category, Long documentId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Validate
        if (!imageEmbeddingService.isValidImage(file)) {
            throw new RuntimeException("Invalid image file format");
        }

        // Create record
        KnowledgeImage image = KnowledgeImage.builder()
                .userId(userId)
                .documentId(documentId)
                .imageName(file.getOriginalFilename())
                .category(category != null ? category : "EXERCISE_DEMO")
                .status("PENDING")
                .build();

        image = imageRepository.save(image);

        // Process async
        processImageAsync(image.getId(), file);

        return image;
    }

    @Async
    public void processImageAsync(Long imageId, MultipartFile file) {
        KnowledgeImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        try {
            // Update status
            image.setStatus("PROCESSING");
            imageRepository.save(image);

            // Send progress
            webSocketService.sendNotification(
                    image.getUserId().toString(),
                    "图片处理中",
                    "正在分析图片内容...",
                    "info"
            );

            // 1. Upload to MinIO
            String imageUrl = minioService.uploadFile(file, imageBucket);
            image.setImageUrl(imageUrl);

            // 2. Analyze image content with Vision API
            String description = imageEmbeddingService.analyzeImageContent(file);
            image.setDescription(description);

            // Extract metadata from description (simple keyword matching)
            extractMetadataFromDescription(image, description);

            // 3. Generate image embedding
            float[] embedding = imageEmbeddingService.generateImageEmbedding(file);

            // 4. Store in Milvus
            String vectorId = storeInMilvus(image, embedding);
            image.setVectorId(vectorId);

            // 5. Index in Elasticsearch
            String esId = indexInElasticsearch(image);
            image.setEsId(esId);

            // Success
            image.setStatus("SUCCESS");
            imageRepository.save(image);

            webSocketService.sendNotification(
                    image.getUserId().toString(),
                    "图片处理完成",
                    "图片已成功添加到知识库",
                    "success"
            );

            log.info("Image processed successfully: imageId={}, vectorId={}, esId={}",
                    imageId, vectorId, esId);

        } catch (Exception e) {
            log.error("Failed to process image: imageId={}", imageId, e);

            image.setStatus("FAILED");
            image.setErrorMessage(e.getMessage());
            imageRepository.save(image);

            webSocketService.sendNotification(
                    image.getUserId().toString(),
                    "图片处理失败",
                    e.getMessage(),
                    "error"
            );
        }
    }

    private void extractMetadataFromDescription(KnowledgeImage image, String description) {
        String lowerDesc = description.toLowerCase();

        // Detect action
        if (lowerDesc.contains("深蹲") || lowerDesc.contains("squat")) {
            image.setDetectedAction("深蹲");
            image.setMuscleGroup("LEGS");
        } else if (lowerDesc.contains("卧推") || lowerDesc.contains("bench press")) {
            image.setDetectedAction("卧推");
            image.setMuscleGroup("CHEST");
        } else if (lowerDesc.contains("硬拉") || lowerDesc.contains("deadlift")) {
            image.setDetectedAction("硬拉");
            image.setMuscleGroup("BACK");
        }

        // Detect difficulty
        if (lowerDesc.contains("初级") || lowerDesc.contains("beginner")) {
            image.setDifficulty("BEGINNER");
        } else if (lowerDesc.contains("高级") || lowerDesc.contains("advanced")) {
            image.setDifficulty("ADVANCED");
        } else {
            image.setDifficulty("INTERMEDIATE");
        }
    }

    private String storeInMilvus(KnowledgeImage image, float[] embedding) {
        try {
            List<InsertParam.Field> fields = new ArrayList<>();

            // Generate unique ID
            String vectorId = "img_" + image.getId() + "_" + System.currentTimeMillis();

            fields.add(new InsertParam.Field("id", Collections.singletonList(vectorId)));
            fields.add(new InsertParam.Field("user_id", Collections.singletonList(image.getUserId())));
            fields.add(new InsertParam.Field("document_id", Collections.singletonList(image.getDocumentId() != null ? image.getDocumentId() : 0L)));
            fields.add(new InsertParam.Field("chunk_index", Collections.singletonList(0)));
            fields.add(new InsertParam.Field("embedding", Collections.singletonList(embedding)));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(milvusCollection)
                    .withFields(fields)
                    .build();

            R<MutationResult> response = milvusClient.insert(insertParam);

            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Milvus insert failed: " + response.getMessage());
            }

            log.info("Image stored in Milvus: vectorId={}", vectorId);
            return vectorId;

        } catch (Exception e) {
            log.error("Failed to store image in Milvus", e);
            throw new RuntimeException("Failed to store image in Milvus", e);
        }
    }

    private String indexInElasticsearch(KnowledgeImage image) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("user_id", image.getUserId());
            doc.put("document_id", image.getDocumentId());
            doc.put("chunk_index", 0);
            doc.put("chunk_text", image.getDescription());
            doc.put("document_name", image.getImageName());
            doc.put("category", image.getCategory());
            doc.put("source_type", "IMAGE");
            doc.put("source_url", image.getImageUrl());
            doc.put("detected_action", image.getDetectedAction());
            doc.put("muscle_group", image.getMuscleGroup());
            doc.put("difficulty", image.getDifficulty());
            doc.put("created_at", image.getCreatedAt().toString());

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(esIndex)
                    .document(doc)
            );

            IndexResponse response = elasticsearchClient.index(request);

            log.info("Image indexed in Elasticsearch: esId={}", response.id());
            return response.id();

        } catch (IOException e) {
            log.error("Failed to index image in Elasticsearch", e);
            throw new RuntimeException("Failed to index image in Elasticsearch", e);
        }
    }

    public List<KnowledgeImage> getUserImages() {
        Long userId = SecurityUtils.getCurrentUserId();
        return imageRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public KnowledgeImage getImageById(Long imageId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return imageRepository.findByIdAndUserId(imageId, userId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
    }

    @Transactional
    public void deleteImage(Long imageId) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeImage image = imageRepository.findByIdAndUserId(imageId, userId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // Delete from MinIO
        try {
            minioService.deleteFile(image.getImageUrl());
        } catch (Exception e) {
            log.warn("Failed to delete image from MinIO: {}", e.getMessage());
        }

        // Delete from Milvus
        // TODO: Implement Milvus delete

        // Delete from Elasticsearch
        // TODO: Implement ES delete

        // Delete from database
        imageRepository.delete(image);

        log.info("Image deleted: imageId={}", imageId);
    }
}
