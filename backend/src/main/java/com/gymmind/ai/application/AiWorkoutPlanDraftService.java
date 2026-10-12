package com.gymmind.ai.application;

import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;

import java.util.List;

public interface AiWorkoutPlanDraftService {
    CreateWorkoutPlanCommand generate(CurrentActor actor, Long memberId, String request);

    AiWorkoutPlanDraftResponse generateDraft(CurrentActor actor, Long memberId, String request, boolean includeHistory);

    AiWorkoutPlanDraftResponse generateDraft(CurrentActor actor, Long memberId, AiWorkoutPlanDraftRequest request);

    List<AiWorkoutPlanTarget> targets(CurrentActor actor);
}
