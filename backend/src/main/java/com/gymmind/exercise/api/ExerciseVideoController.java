package com.gymmind.exercise.api;

import com.gymmind.exercise.application.CreateExerciseVideoCommand;
import com.gymmind.exercise.application.ExerciseVideoService;
import com.gymmind.exercise.application.ExerciseVideoView;
import com.gymmind.exercise.domain.model.ExerciseVideoSourceType;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/videos")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseVideoController {
    private final ExerciseVideoService service;
    private final CurrentActorProvider actors;

    public ExerciseVideoController(ExerciseVideoService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExerciseVideoView> create(@Valid @RequestBody VideoRequest request) {
        return ApiResponse.success(service.create(actors.requireCurrent(), request.toCommand()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExerciseVideoView> find(@PathVariable Long id) {
        return ApiResponse.success(service.find(actors.requireCurrent(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(actors.requireCurrent(), id);
        return ApiResponse.success(null);
    }

    public record VideoRequest(@NotNull Long exerciseId, @NotBlank String title, @NotBlank String platform,
                               String videoUrl, String objectKey, @NotBlank String author, int duration,
                               @NotBlank String description, @NotNull ExerciseVideoSourceType sourceType) {
        CreateExerciseVideoCommand toCommand() {
            return new CreateExerciseVideoCommand(null, exerciseId, title, platform, videoUrl, objectKey, author,
                    duration, description, sourceType);
        }
    }
}
