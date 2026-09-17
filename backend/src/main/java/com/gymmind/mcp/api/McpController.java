package com.gymmind.mcp.api;

import com.gymmind.mcp.application.McpTool;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mcp")
public class McpController {

    private final McpToolService service;
    private final CurrentActorProvider actors;

    public McpController(McpToolService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping("/tools")
    public ApiResponse<List<McpTool>> tools() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @PostMapping("/invoke")
    public ApiResponse<Object> invoke(@Valid @RequestBody Request request) {
        return ApiResponse.success(service.invoke(actors.requireCurrent(), request.name(), request.input()));
    }

    public record Request(@NotBlank String name, @NotBlank String input) {
    }
}
