package com.gymmind.ai.api;

import com.gymmind.ai.application.AiWorkoutPlanDraftService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiWorkoutPlanController {
    private final AiWorkoutPlanDraftService service;
    private final CurrentActorProvider actors;

    public AiWorkoutPlanController(AiWorkoutPlanDraftService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping("/workout-plan-drafts")
    public ApiResponse<CreateWorkoutPlanCommand> generate(@RequestBody DraftRequest request) {
        return ApiResponse.success(service.generate(actors.requireCurrent(), request.memberId(), request.request()));
    }

    public record DraftRequest(Long memberId, String request) {}
}
