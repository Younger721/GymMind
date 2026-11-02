package com.gymmind.mcp.api;

import com.gymmind.mcp.application.McpClientService;
import com.gymmind.mcp.application.McpTool;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
public class McpController {

    private final McpToolService service;
    private final CurrentActorProvider actors;
    private final McpClientService mcpClients;

    public McpController(McpToolService service, CurrentActorProvider actors, McpClientService mcpClients) {
        this.service = service;
        this.actors = actors;
        this.mcpClients = mcpClients;
    }

    @GetMapping("/tools")
    public ApiResponse<List<McpTool>> tools() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    /** 远程 MCP server 的可用技能清单（AgentManageView 配置 enabledTools 的参考） */
    @GetMapping("/remote-tools")
    public ApiResponse<List<com.gymmind.mcp.application.RemoteMcpTool>> remoteTools() {
        ActorAccess.requireGymAdmin(actors.requireCurrent(), "agent:write");
        return ApiResponse.success(mcpClients.listRemoteTools());
    }

    @PostMapping("/invoke")
    public ApiResponse<Object> invoke(@Valid @RequestBody Request request) {
        return ApiResponse.success(service.invoke(actors.requireCurrent(), request.name(), request.input()));
    }

    public record Request(@NotBlank String name, @NotBlank String input) {
    }
}
