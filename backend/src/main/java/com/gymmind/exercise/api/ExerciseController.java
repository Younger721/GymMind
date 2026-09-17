package com.gymmind.exercise.api;

import com.gymmind.exercise.application.CreateExerciseCommand;
import com.gymmind.exercise.application.ExerciseService;
import com.gymmind.exercise.application.ExerciseUpdateCommand;
import com.gymmind.exercise.application.ExerciseView;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exercises")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseController {
    private final ExerciseService service;
    private final CurrentActorProvider actors;

    public ExerciseController(ExerciseService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExerciseView> create(@Valid @RequestBody ExerciseRequest request) {
        return ApiResponse.success(service.create(actors.requireCurrent(), request.toCommand()));
    }

    @GetMapping
    public ApiResponse<java.util.List<ExerciseView>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExerciseView> find(@PathVariable Long id) {
        return ApiResponse.success(service.find(actors.requireCurrent(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ExerciseRequest request) {
        service.update(actors.requireCurrent(), id, request.toUpdateCommand());
        return ApiResponse.success(null);
    }

    public record ExerciseRequest(@NotBlank String name, @NotBlank String category, @NotBlank String targetMuscle,
                                  @NotBlank String difficulty, @NotBlank String equipment, @NotBlank String description,
                                  @NotBlank String steps, @NotBlank String commonMistakes, @NotBlank String safetyNotes,
                                  @NotBlank String tags) {
        CreateExerciseCommand toCommand() {
            return new CreateExerciseCommand(null, name, category, targetMuscle, difficulty, equipment, description,
                    steps, commonMistakes, safetyNotes, tags);
        }
        ExerciseUpdateCommand toUpdateCommand() {
            return new ExerciseUpdateCommand(name, category, targetMuscle, difficulty, equipment, description,
                    steps, commonMistakes, safetyNotes, tags);
        }
    }
}
