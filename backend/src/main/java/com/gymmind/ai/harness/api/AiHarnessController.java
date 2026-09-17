package com.gymmind.ai.harness.api;

import com.gymmind.ai.application.AiChatView;
import com.gymmind.ai.harness.application.AiHarnessService;
import com.gymmind.ai.harness.application.HarnessConfigView;
import com.gymmind.ai.harness.application.UpdateHarnessConfigCommand;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/harness")
@SecurityRequirement(name = "bearerAuth")
public class AiHarnessController {

    private final AiHarnessService service;
    private final CurrentActorProvider actors;

    public AiHarnessController(AiHarnessService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    /** 获取当前用户可见的 Harness 配置与插件目录 */
    @GetMapping("/config")
    public ApiResponse<HarnessConfigView> config() {
        return ApiResponse.success(service.getConfig(actors.requireCurrent()));
    }

    /** 租户管理员更新本店 Harness 配置 */
    @PutMapping("/config")
    public ApiResponse<HarnessConfigView> updateConfig(@Valid @RequestBody ConfigRequest request) {
        return ApiResponse.success(service.updateConfig(
                actors.requireCurrent(),
                new UpdateHarnessConfigCommand(request.enabledPlugins(), request.systemPrompt())));
    }

    /** 插件化对话：按已启用插件组装上下文后调用模型 */
    @PostMapping("/chat")
    public ApiResponse<AiChatView> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(service.chat(
                actors.requireCurrent(),
                request.sessionId(),
                request.question(),
                request.plugins()));
    }

    public record ConfigRequest(List<String> enabledPlugins, @NotBlank String systemPrompt) {
    }

    public record ChatRequest(
            @NotBlank String sessionId,
            @NotBlank String question,
            List<String> plugins) {
    }
}
