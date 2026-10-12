package com.gymmind.ai.harness.api;

import com.gymmind.ai.application.AiChatView;
import com.gymmind.ai.harness.application.AiHarnessService;
import com.gymmind.ai.harness.application.HarnessConfigView;
import com.gymmind.ai.harness.application.UpdateHarnessConfigCommand;
import com.gymmind.ai.harness.application.AiChatTurn;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.shared.security.CurrentActor;
import org.slf4j.MDC;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;

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

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.http.ResponseEntity<SseEmitter> chatStream(@Valid @RequestBody ChatRequest request) {
        CurrentActor actor = actors.requireCurrent();
        SseEmitter emitter = new SseEmitter(300_000L);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "ai-harness-stream");
            thread.setDaemon(true);
            return thread;
        });
        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(() -> {
            executor.shutdownNow();
            emitter.complete();
        });
        executor.execute(() -> {
            try {
                AiChatView result = service.chat(actor, request.sessionId(), request.question(), request.plugins(), request.history(),
                        token -> sendToken(emitter, token),
                        progress -> sendProgress(emitter, progress));
                emitter.send(SseEmitter.event().name("done").data(result));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(buildStreamError(ex)));
                } catch (Exception ignored) {
                    // 客户端已断开连接
                }
                emitter.complete();
            }
        });
        return org.springframework.http.ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    private static Map<String, String> buildStreamError(Exception ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "AI 服务暂时不可用"
                : ex.getMessage();
        String code = ex instanceof BusinessException business
                ? business.errorCode().name()
                : ErrorCode.DEPENDENCY_UNAVAILABLE.name();
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            return Map.of("message", message, "code", code);
        }
        return Map.of("message", message, "code", code, "traceId", traceId);
    }

    private static void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().name("token").data(token));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Client disconnected during AI response", exception);
        }
    }

    private static void sendProgress(SseEmitter emitter, AiProgressEvent progress) {
        try {
            String eventName = switch (progress.stage()) {
                case "tool_decision", "tool_start", "tool_result" -> progress.stage();
                default -> "progress";
            };
            emitter.send(SseEmitter.event().name(eventName).data(progress));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Client disconnected during AI progress", exception);
        }
    }

    public record ConfigRequest(List<String> enabledPlugins, @NotBlank String systemPrompt) {
    }

    public record ChatRequest(
            @NotBlank String sessionId,
            @NotBlank String question,
            List<String> plugins,
            List<AiChatTurn> history) {
    }
}
