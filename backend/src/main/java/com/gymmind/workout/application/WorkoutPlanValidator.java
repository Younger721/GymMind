package com.gymmind.workout.application;
import com.gymmind.shared.error.*;
public class WorkoutPlanValidator {
    public void validate(CreateWorkoutPlanCommand c) {
        if (c == null || c.memberId() == null || c.name() == null || c.goal() == null || c.description() == null
                || c.startDate() == null || c.endDate() == null || c.items() == null || c.items().isEmpty()
                || c.endDate().isBefore(c.startDate())) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        for (WorkoutPlanItemCommand i : c.items()) {
            if (i == null || i.exerciseId() == null || i.dayOfWeek() < 1 || i.dayOfWeek() > 7 || i.sets() <= 0
                    || i.reps() <= 0 || i.restSeconds() < 0 || i.weight() < 0) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
