package com.gymmind.workout.api;

import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.CreateWorkoutPlanCommand;
import com.gymmind.workout.application.WorkoutPlanItemCommand;
import com.gymmind.workout.application.WorkoutPlanService;
import com.gymmind.workout.application.WorkoutPlanView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workout-plans")
public class WorkoutPlanController {

    private final WorkoutPlanService service;
    private final CurrentActorProvider actors;

    public WorkoutPlanController(WorkoutPlanService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkoutPlanView> create(@Valid @RequestBody Request request) {
        return ApiResponse.success(service.create(actors.requireCurrent(), request.toCommand()));
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkoutPlanView> saveDraft(@Valid @RequestBody Request request) {
        return ApiResponse.success(service.saveValidatedDraft(actors.requireCurrent(), request.toCommand()));
    }

    @GetMapping
    public ApiResponse<List<WorkoutPlanView>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkoutPlanView> find(@PathVariable Long id) {
        return ApiResponse.success(service.find(actors.requireCurrent(), id));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<WorkoutPlanView> publish(@PathVariable Long id) {
        return ApiResponse.success(service.publish(actors.requireCurrent(), id));
    }

    public record Request(
            @NotNull Long memberId,
            Long coachId,
            @NotBlank String name,
            @NotBlank String goal,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank String description,
            @NotEmpty List<RequestItem> items) {
        CreateWorkoutPlanCommand toCommand() {
            return new CreateWorkoutPlanCommand(null, memberId, coachId, name, goal, startDate, endDate, description,
                    items.stream().map(RequestItem::toCommand).toList());
        }
    }

    public record RequestItem(
            @NotNull Long exerciseId,
            @Min(1) @Max(7) int dayOfWeek,
            @Min(1) int sets,
            @Min(1) int reps,
            @Min(0) int restSeconds,
            @Min(0) double weight,
            String notes) {
        WorkoutPlanItemCommand toCommand() {
            return new WorkoutPlanItemCommand(exerciseId, dayOfWeek, sets, reps, restSeconds, weight, notes);
        }
    }
}
