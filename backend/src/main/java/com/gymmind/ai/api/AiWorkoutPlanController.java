package com.gymmind.ai.api;

import com.gymmind.ai.application.AiWorkoutPlanDraftService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import com.gymmind.workout.application.WorkoutPlanService;
import com.gymmind.workout.application.WorkoutPlanView;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1/ai")
public class AiWorkoutPlanController {
    private final AiWorkoutPlanDraftService service;
    private final CurrentActorProvider actors;
    private final WorkoutPlanService workoutPlans;

    public AiWorkoutPlanController(AiWorkoutPlanDraftService service, CurrentActorProvider actors) {
        this(service, actors, null);
    }

    @Autowired
    public AiWorkoutPlanController(AiWorkoutPlanDraftService service, CurrentActorProvider actors, WorkoutPlanService workoutPlans) {
        this.service = service;
        this.actors = actors;
        this.workoutPlans = workoutPlans;
    }

    @PostMapping("/workout-plan-drafts")
    public ApiResponse<CreateWorkoutPlanCommand> generate(@RequestBody DraftRequest request) {
        return ApiResponse.success(service.generate(actors.requireCurrent(), request.memberId(), request.request()));
    }

    @PostMapping("/workout-plan")
    public ApiResponse<WorkoutPlanView> generateAndSave(@RequestBody DraftRequest request) {
        var actor = actors.requireCurrent();
        if (workoutPlans == null) throw new IllegalStateException("workout plan service unavailable");
        var draft = service.generate(actor, request.memberId(), request.request());
        return ApiResponse.success(workoutPlans.saveValidatedDraft(actor, draft));
    }

    public record DraftRequest(Long memberId, String request) {}
}
