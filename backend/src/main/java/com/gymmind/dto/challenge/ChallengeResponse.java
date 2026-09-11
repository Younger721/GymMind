package com.gymmind.dto.challenge;

import lombok.Data;

@Data
public class ChallengeResponse {
    private Long id;
    private Long creatorId;
    private String name;
    private String description;
    private String goalType;
    private Integer goalValue;
    private String startDate;
    private String endDate;
    private String status;
    private Integer participantCount;
    private String imageUrl;
    private String difficulty;
    private Boolean isParticipating;
    private Integer userProgress;
    private String createdAt;
}
