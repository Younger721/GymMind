package com.gymmind.ai.application;

import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;

public interface AiWorkoutPlanDraftService {
    CreateWorkoutPlanCommand generate(CurrentActor actor, Long memberId, String request);
}
