package com.gymmind.ai.api;

import com.gymmind.ai.application.RecommendationCandidate;
import com.gymmind.ai.application.RecommendationService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ai/recommendations")
@SecurityRequirement(name = "bearerAuth")
public class RecommendationController {
    private final RecommendationService service;
    private final CurrentActorProvider actors;

    public RecommendationController(RecommendationService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    public ApiResponse<List<RecommendationCandidate>> recommend(@RequestBody Request request) {
        Set<String> constraints = request.healthConstraints() == null ? Set.of() : Set.copyOf(request.healthConstraints());
        return ApiResponse.success(service.recommend(actors.requireCurrent(), request.goal(), constraints));
    }

    public record Request(String goal, Set<String> healthConstraints) { }
}
