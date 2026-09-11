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
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String imageUrls; // JSON array of image URLs

    @Column(nullable = false)
    private Integer likes;

    @Column(nullable = false)
    private Integer comments;

    @Column(length = 50)
    private String postType; // WORKOUT, PROGRESS, MEAL, GENERAL

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON: workout details, nutrition info, etc.

    @CreationTimestamp
    private LocalDateTime createdAt;
}
