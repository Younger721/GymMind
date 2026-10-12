package com.gymmind.ai.api;

import com.gymmind.ai.application.AiWorkoutPlanDraftService;
import com.gymmind.ai.application.AiWorkoutPlanDraftResponse;
import com.gymmind.ai.application.AiWorkoutPlanTarget;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import com.gymmind.workout.application.WorkoutPlanService;
import com.gymmind.workout.application.WorkoutPlanView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@SecurityRequirement(name = "bearerAuth")
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

    @GetMapping("/workout-plan-targets")
    public ApiResponse<List<AiWorkoutPlanTarget>> targets() {
        return ApiResponse.success(service.targets(actors.requireCurrent()));
    }

    @PostMapping("/workout-plan-drafts")
    public ApiResponse<AiWorkoutPlanDraftResponse> generateDraft(@RequestBody DraftRequest request) {
        boolean includeHistory = request.includeHistory() == null || request.includeHistory();
        var draftRequest = new com.gymmind.ai.application.AiWorkoutPlanDraftRequest(
                request.request(),
                includeHistory,
                request.goal(),
                request.sessionsPerWeek(),
                request.experience());
        return ApiResponse.success(service.generateDraft(actors.requireCurrent(), request.memberId(), draftRequest));
    }

    @PostMapping("/workout-plan")
    public ApiResponse<WorkoutPlanView> generateAndSave(@RequestBody DraftRequest request) {
        var actor = actors.requireCurrent();
        if (workoutPlans == null) throw new IllegalStateException("workout plan service unavailable");
        var draft = service.generate(actor, request.memberId(), request.request());
        return ApiResponse.success(workoutPlans.saveValidatedDraft(actor, draft));
    }

    public record DraftRequest(
            Long memberId,
            String request,
            Boolean includeHistory,
            String goal,
            Integer sessionsPerWeek,
            String experience) {
        public DraftRequest(Long memberId, String request) {
            this(memberId, request, true, null, null, null);
        }
    }
}
