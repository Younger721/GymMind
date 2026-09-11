package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "challenge_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"challenge_id", "user_id"})
})
public class ChallengeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "challenge_id")
    private Long challengeId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private Integer progress;

    @Column(nullable = false)
    private String status; // ACTIVE, COMPLETED, ABANDONED

    @Column(nullable = false)
    private Integer rank;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
