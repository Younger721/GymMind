package com.gymmind.ai.api;

import com.gymmind.ai.application.AiMemoryEntry;
import com.gymmind.ai.application.AiMemoryService;
import com.gymmind.ai.application.AiMemoryType;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/memories")
@SecurityRequirement(name = "bearerAuth")
public class AiMemoryController {
    private final AiMemoryService service;
    private final CurrentActorProvider actors;

    public AiMemoryController(AiMemoryService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    public ApiResponse<Void> write(@Valid @RequestBody WriteRequest request) {
        service.write(actors.requireCurrent(), request.type(), request.key(), request.value());
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<AiMemoryEntry>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam AiMemoryType type, @RequestParam String key) {
        service.delete(actors.requireCurrent(), type, key);
        return ApiResponse.success(null);
    }

    public record WriteRequest(@NotNull AiMemoryType type, @NotBlank String key, @NotBlank String value) {
    }
}
