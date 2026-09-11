package com.gymmind.dto.challenge;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ParticipantResponse {
    private Long userId;
    private String username;
    private Integer progress;
    private String status;
    private Integer rank;
    private LocalDateTime joinedAt;
}
