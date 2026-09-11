package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.challenge.ChallengeResponse;
import com.gymmind.dto.challenge.CreateChallengeRequest;
import com.gymmind.dto.challenge.ParticipantResponse;
import com.gymmind.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping
    public ApiResponse<ChallengeResponse> createChallenge(@RequestBody CreateChallengeRequest request) {
        ChallengeResponse challenge = challengeService.createChallenge(request);
        return ApiResponse.success(challenge);
    }

    @GetMapping("/active")
    public ApiResponse<List<ChallengeResponse>> getActiveChallenges() {
        List<ChallengeResponse> challenges = challengeService.getActiveChallenges();
        return ApiResponse.success(challenges);
    }

    @GetMapping("/user")
    public ApiResponse<List<ChallengeResponse>> getUserChallenges() {
        List<ChallengeResponse> challenges = challengeService.getUserChallenges();
        return ApiResponse.success(challenges);
    }

    @PostMapping("/{challengeId}/join")
    public ApiResponse<Void> joinChallenge(@PathVariable Long challengeId) {
        challengeService.joinChallenge(challengeId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{challengeId}/progress")
    public ApiResponse<Void> updateProgress(
            @PathVariable Long challengeId,
            @RequestParam Integer progress) {
        challengeService.updateProgress(challengeId, progress);
        return ApiResponse.success(null);
    }

    @GetMapping("/{challengeId}/leaderboard")
    public ApiResponse<List<ParticipantResponse>> getLeaderboard(@PathVariable Long challengeId) {
        List<ParticipantResponse> leaderboard = challengeService.getLeaderboard(challengeId);
        return ApiResponse.success(leaderboard);
    }
}
