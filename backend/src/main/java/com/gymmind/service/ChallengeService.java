package com.gymmind.service;

import com.gymmind.dto.challenge.ChallengeResponse;
import com.gymmind.dto.challenge.CreateChallengeRequest;
import com.gymmind.dto.challenge.ParticipantResponse;
import com.gymmind.entity.Challenge;
import com.gymmind.entity.ChallengeParticipant;
import com.gymmind.entity.User;
import com.gymmind.repository.ChallengeParticipantRepository;
import com.gymmind.repository.ChallengeRepository;
import com.gymmind.repository.UserRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    @Transactional
    public ChallengeResponse createChallenge(CreateChallengeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Challenge challenge = Challenge.builder()
                .creatorId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .goalType(request.getGoalType())
                .goalValue(request.getGoalValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(LocalDate.now().isBefore(request.getStartDate()) ? "UPCOMING" : "ACTIVE")
                .participantCount(1)
                .imageUrl(request.getImageUrl())
                .difficulty(request.getDifficulty())
                .build();

        challenge = challengeRepository.save(challenge);

        // Creator automatically joins
        ChallengeParticipant participant = ChallengeParticipant.builder()
                .challengeId(challenge.getId())
                .userId(userId)
                .progress(0)
                .status("ACTIVE")
                .rank(1)
                .build();

        participantRepository.save(participant);

        log.info("Challenge created: challengeId={}, creatorId={}", challenge.getId(), userId);

        return convertToResponse(challenge, userId);
    }

    public List<ChallengeResponse> getActiveChallenges() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Challenge> challenges = challengeRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");

        return challenges.stream()
                .map(challenge -> convertToResponse(challenge, userId))
                .collect(Collectors.toList());
    }

    public List<ChallengeResponse> getUserChallenges() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ChallengeParticipant> participants = participantRepository.findByUserId(userId);

        return participants.stream()
                .map(participant -> {
                    Challenge challenge = challengeRepository.findById(participant.getChallengeId())
                            .orElse(null);
                    return challenge != null ? convertToResponse(challenge, userId) : null;
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    @Transactional
    public void joinChallenge(Long challengeId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Check if already joined
        if (participantRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            throw new RuntimeException("Already joined this challenge");
        }

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        ChallengeParticipant participant = ChallengeParticipant.builder()
                .challengeId(challengeId)
                .userId(userId)
                .progress(0)
                .status("ACTIVE")
                .rank(challenge.getParticipantCount() + 1)
                .build();

        participantRepository.save(participant);

        // Update participant count
        challenge.setParticipantCount(challenge.getParticipantCount() + 1);
        challengeRepository.save(challenge);

        log.info("User joined challenge: challengeId={}, userId={}", challengeId, userId);
    }

    @Transactional
    public void updateProgress(Long challengeId, Integer progress) {
        Long userId = SecurityUtils.getCurrentUserId();

        ChallengeParticipant participant = participantRepository.findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new RuntimeException("Not participating in this challenge"));

        participant.setProgress(progress);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        // Check if completed
        if (progress >= challenge.getGoalValue()) {
            participant.setStatus("COMPLETED");

            webSocketService.sendNotification(
                    userId.toString(),
                    "挑战完成！",
                    "恭喜你完成了挑战：" + challenge.getName(),
                    "success"
            );
        }

        participantRepository.save(participant);

        // Update rankings
        updateRankings(challengeId);

        log.info("Challenge progress updated: challengeId={}, userId={}, progress={}",
                challengeId, userId, progress);
    }

    public List<ParticipantResponse> getLeaderboard(Long challengeId) {
        List<ChallengeParticipant> participants = participantRepository
                .findByChallengeIdOrderByProgressDescRankAsc(challengeId);

        return participants.stream()
                .map(participant -> {
                    User user = userRepository.findById(participant.getUserId()).orElse(null);

                    return ParticipantResponse.builder()
                            .userId(participant.getUserId())
                            .username(user != null ? user.getUsername() : "Unknown")
                            .progress(participant.getProgress())
                            .status(participant.getStatus())
                            .rank(participant.getRank())
                            .joinedAt(participant.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void updateRankings(Long challengeId) {
        List<ChallengeParticipant> participants = participantRepository
                .findByChallengeIdOrderByProgressDescRankAsc(challengeId);

        int rank = 1;
        for (ChallengeParticipant participant : participants) {
            participant.setRank(rank++);
            participantRepository.save(participant);
        }
    }

    private ChallengeResponse convertToResponse(Challenge challenge, Long currentUserId) {
        boolean isParticipating = participantRepository.existsByChallengeIdAndUserId(
                challenge.getId(), currentUserId);

        Integer userProgress = null;
        if (isParticipating) {
            ChallengeParticipant participant = participantRepository
                    .findByChallengeIdAndUserId(challenge.getId(), currentUserId)
                    .orElse(null);
            userProgress = participant != null ? participant.getProgress() : 0;
        }

        return ChallengeResponse.builder()
                .id(challenge.getId())
                .creatorId(challenge.getCreatorId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .goalType(challenge.getGoalType())
                .goalValue(challenge.getGoalValue())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .status(challenge.getStatus())
                .participantCount(challenge.getParticipantCount())
                .imageUrl(challenge.getImageUrl())
                .difficulty(challenge.getDifficulty())
                .isParticipating(isParticipating)
                .userProgress(userProgress)
                .createdAt(challenge.getCreatedAt())
                .build();
    }
}
