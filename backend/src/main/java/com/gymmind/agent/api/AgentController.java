package com.gymmind.agent.api;

import com.gymmind.agent.application.AgentManagementService;
import com.gymmind.agent.application.AgentView;
import com.gymmind.agent.application.CreateAgentCommand;
import com.gymmind.agent.application.UpdateAgentCommand;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentManagementService service;
    private final CurrentActorProvider actors;

    public AgentController(AgentManagementService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping
    public ApiResponse<List<AgentView>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentView> find(@PathVariable Long id) {
        return ApiResponse.success(service.find(actors.requireCurrent(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AgentView> create(@Valid @RequestBody AgentRequest request) {
        return ApiResponse.success(service.create(actors.requireCurrent(), request.toCreateCommand()));
    }

    @PutMapping("/{id}")
    public ApiResponse<AgentView> update(@PathVariable Long id, @Valid @RequestBody AgentRequest request) {
        return ApiResponse.success(service.update(actors.requireCurrent(), id, request.toUpdateCommand()));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<AgentView> disable(@PathVariable Long id) {
        return ApiResponse.success(service.disable(actors.requireCurrent(), id));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<AgentView> activate(@PathVariable Long id) {
        return ApiResponse.success(service.activate(actors.requireCurrent(), id));
    }

    public record AgentRequest(
            @NotBlank String name,
            String description,
            @NotBlank String systemPrompt,
            boolean knowledgeEnabled,
            @NotEmpty List<String> enabledTools) {
        CreateAgentCommand toCreateCommand() {
            return new CreateAgentCommand(name, description, systemPrompt, knowledgeEnabled, enabledTools);
        }

        UpdateAgentCommand toUpdateCommand() {
            return new UpdateAgentCommand(name, description, systemPrompt, knowledgeEnabled, enabledTools);
        }
    }
}
