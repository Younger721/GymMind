package com.gymmind.ai.harness.api;

import com.gymmind.ai.harness.application.AiHarnessService;
import com.gymmind.ai.harness.application.HarnessConfigView;
import com.gymmind.ai.harness.application.UpdateHarnessConfigCommand;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/ai/harness")
@SecurityRequirement(name = "bearerAuth")
public class PlatformAiHarnessController {

    private final AiHarnessService service;
    private final CurrentActorProvider actors;

    public PlatformAiHarnessController(AiHarnessService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    /** 平台级 Harness 默认配置（新租户继承 + 平台管理员沙箱） */
    @GetMapping("/config")
    public ApiResponse<HarnessConfigView> config() {
        return ApiResponse.success(service.getPlatformConfig(actors.requireCurrent()));
    }

    @PutMapping("/config")
    public ApiResponse<HarnessConfigView> updateConfig(@Valid @RequestBody ConfigRequest request) {
        return ApiResponse.success(service.updatePlatformConfig(
                actors.requireCurrent(),
                new UpdateHarnessConfigCommand(request.enabledPlugins(), request.systemPrompt())));
    }

    public record ConfigRequest(List<String> enabledPlugins, @NotBlank String systemPrompt) {
    }
}
