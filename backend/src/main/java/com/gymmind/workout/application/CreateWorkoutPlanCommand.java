package com.gymmind.workout.application;
import java.time.LocalDate; import java.util.List;
public record CreateWorkoutPlanCommand(Long tenantId, Long memberId, Long coachId, String name, String goal,
                                       LocalDate startDate, LocalDate endDate, String description, List<WorkoutPlanItemCommand> items) {}
