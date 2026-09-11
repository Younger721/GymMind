package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_images")
public class KnowledgeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long documentId; // Reference to parent document (optional)

    @Column(nullable = false, length = 200)
    private String imageName;

    @Column(nullable = false, length = 500)
    private String imageUrl; // MinIO URL

    @Column(columnDefinition = "TEXT")
    private String description; // AI-generated description

    @Column(length = 100)
    private String category; // EXERCISE_DEMO, BODY_MEASUREMENT, MEAL, PROGRESS_PHOTO

    @Column(length = 100)
    private String detectedAction; // e.g., "深蹲", "卧推"

    @Column(length = 100)
    private String muscleGroup; // CHEST, BACK, LEGS, etc.

    @Column(length = 50)
    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(nullable = false)
    private String status; // PENDING, PROCESSING, SUCCESS, FAILED

    @Column(length = 500)
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "vector_id")
    private String vectorId; // ID in Milvus

    @Column(name = "es_id")
    private String esId; // ID in Elasticsearch
}
