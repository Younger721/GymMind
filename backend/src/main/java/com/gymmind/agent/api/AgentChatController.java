package com.gymmind.agent.api;

import com.gymmind.agent.application.AgentChatService;
import com.gymmind.ai.application.AiChatView;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
@SecurityRequirement(name = "bearerAuth")
public class AgentChatController {

    private final AgentChatService service;
    private final CurrentActorProvider actors;

    public AgentChatController(AgentChatService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping("/{id}/chat")
    public ApiResponse<AiChatView> chat(@PathVariable Long id, @Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(service.chat(
                actors.requireCurrent(), id, request.sessionId(), request.question()));
    }

    public record ChatRequest(@NotBlank String sessionId, @NotBlank String question) {
    }
}
