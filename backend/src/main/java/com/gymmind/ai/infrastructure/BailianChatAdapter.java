package com.gymmind.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.application.ChatModelCallResult;
import com.gymmind.ai.application.ModelUsage;
import com.gymmind.ai.application.ChatModelResponse;
import com.gymmind.ai.application.ChatToolCall;
import com.gymmind.ai.application.ChatToolDefinition;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.infrastructure.bailian.DashScopeProperties;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.web.client.ResourceAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bailian OpenAI-compatible chat adapter with a deterministic local fallback. */
@Component
public class BailianChatAdapter implements ChatModelGateway {
    private static final Logger log = LoggerFactory.getLogger(BailianChatAdapter.class);
    private static final String QUICK_SYSTEM_PROMPT = "你是 GymMind AI。必须严格遵守用户请求中标记为【当前身份规则】的角色边界，不得自行把平台管理员、门店管理员或教练当成会员。涉及签到、预约、会籍、训练记录或门店经营数据时，优先调用提供的只读工具，不要凭空估计或要求用户提供后端已有的身份、租户和日期信息；工具返回数据后只根据工具结果回答。直接回答，不要输出分析过程、思考步骤、检查过程或草稿；普通问题限制为 1～3 句话，总长度控制在 120 个中文字符以内。先给结论，再补充最多 2 条必要说明。信息不足时简短说明依据不足；只有纯 AI 模式且用户明确要求个性化方案时，才提出必要问题。若用户消息包含【RAG回答规则】，必须直接依据参考资料回答，不要向用户追问。价格和优惠问题必须列出文档名、版本和原文依据；基础价格与促销规则同时存在时明确写出计算过程，默认不叠加冲突优惠。不得编造。";
    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    @Autowired
    public BailianChatAdapter(RestClient.Builder builder, DashScopeProperties dashScope) {
        this(builder, dashScope.baseUrlForChat(), dashScope.apiKeyForChat(), dashScope.getChatModel());
    }

    BailianChatAdapter(RestClient.Builder builder, String apiKey, String model) {
        this(builder, "https://dashscope.aliyuncs.com/compatible-mode/v1", apiKey, model);
    }

    private BailianChatAdapter(RestClient.Builder builder, String baseUrl, String apiKey, String model) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "qwen3.7-plus" : model.trim();
    }

    @Override
    public String complete(String prompt, List<ContextSegment> context) {
        return completeWithUsage(prompt, context).content();
    }

    @Override
    public ChatModelCallResult completeWithUsage(String prompt, List<ContextSegment> context) {
        long started = System.currentTimeMillis();
        if (apiKey.isBlank()) {
            throw providerUnavailable();
        }
        String contextText = context.stream().map(ContextSegment::text).reduce("", (a, b) -> a + "\n" + b);
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 256,
                "enable_thinking", false,
                "messages", List.of(
                Map.of("role", "system", "content", quickSystemPrompt()),
                        Map.of("role", "user", "content", prompt + "\n参考资料：" + contextText)));
        try {
            String raw = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body).retrieve().body(String.class);
            JsonNode root = mapper.readTree(raw);
            ModelUsage usage = BailianUsageParser.parse(root);
            String answer = root.path("choices").path(0).path("message").path("content").asText(raw);
            return new ChatModelCallResult(answer, usage, elapsed(started), "OK");
        } catch (RuntimeException | java.io.IOException failure) {
            throw providerUnavailable();
        }
    }

    @Override
    public boolean supportsToolCalling() {
        // DashScope supports OpenAI-compatible tool calling. Credentials are
        // validated when a request is sent so a missing key cannot trigger the
        // legacy deterministic plugin path.
        return true;
    }

    @Override
    public ChatModelResponse completeWithTools(String prompt, List<ContextSegment> context,
                                               List<ChatToolDefinition> tools) {
        if (apiKey.isBlank()) {
            throw providerUnavailable();
        }
        if (tools == null || tools.isEmpty()) {
            ChatModelCallResult direct = completeWithUsage(prompt, context);
            return ChatModelResponse.direct(direct.content(), direct.usage());
        }
        String contextText = context.stream().map(ContextSegment::text).reduce("", (a, b) -> a + "\n" + b);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 256);
        body.put("enable_thinking", false);
        body.put("tool_choice", "auto");
        body.put("messages", List.of(
                Map.of("role", "system", "content", quickSystemPrompt()),
                Map.of("role", "user", "content", prompt + "\n参考资料：" + contextText)));
        body.put("tools", tools.stream().map(tool -> Map.of(
                "type", "function",
                "function", Map.of("name", tool.name(), "description", tool.description(), "parameters", tool.parameters())))
                .toList());
        try {
            String raw = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body).retrieve().body(String.class);
            JsonNode message = mapper.readTree(raw).path("choices").path(0).path("message");
            List<ChatToolCall> calls = new ArrayList<>();
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray()) {
                for (JsonNode call : toolCalls) {
                    String name = call.path("function").path("name").asText("");
                    String rawArguments = call.path("function").path("arguments").asText("{}");
                    if (!name.isBlank()) calls.add(new ChatToolCall(name, mapper.readTree(rawArguments)));
                }
            }
            String content = message.path("content").asText("");
            ModelUsage usage = BailianUsageParser.parse(mapper.readTree(raw));
            return calls.isEmpty() ? ChatModelResponse.direct(content, usage) : ChatModelResponse.toolCalls(calls, usage);
        } catch (BusinessException failure) {
            throw failure;
        } catch (RuntimeException | java.io.IOException failure) {
            log.warn("DashScope completeWithTools failed: {}", failure.toString());
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "阿里百炼工具调用失败：" + safeFailureHint(failure));
        }
    }

    @Override
    public String stream(String prompt, List<ContextSegment> context, Consumer<String> onToken) {
        return streamWithUsage(prompt, context, onToken).content();
    }

    @Override
    public ChatModelCallResult streamWithUsage(String prompt, List<ContextSegment> context, Consumer<String> onToken) {
        long started = System.currentTimeMillis();
        if (apiKey.isBlank()) {
            throw providerUnavailable();
        }
        String contextText = context.stream().map(ContextSegment::text).reduce("", (a, b) -> a + "\n" + b);
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", true,
                "stream_options", Map.of("include_usage", true),
                "max_tokens", 256,
                "enable_thinking", false,
                "messages", List.of(
                Map.of("role", "system", "content", quickSystemPrompt()),
                        Map.of("role", "user", "content", prompt + "\n参考资料：" + contextText)));
        StringBuilder answer = new StringBuilder();
        AtomicBoolean emitted = new AtomicBoolean();
        ModelUsage[] finalUsage = {BailianUsageParser.parse(mapper.createObjectNode())};
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                client.post().uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .body(body).exchange((request, response) -> {
                            if (!response.getStatusCode().is2xxSuccessful()) {
                                throw new IllegalStateException("Model service returned " + response.getStatusCode());
                            }
                            var raw = response.getBody();
                            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(raw, java.nio.charset.StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (!line.startsWith("data:")) continue;
                                    String data = line.substring(5).trim();
                                    if (data.isEmpty() || "[DONE]".equals(data)) continue;
                                    JsonNode root = mapper.readTree(data);
                                    ModelUsage parsedUsage = BailianUsageParser.parse(root);
                                    if (parsedUsage.available()) {
                                        finalUsage[0] = parsedUsage.model() == null
                                                ? new ModelUsage("dashscope", model, parsedUsage.inputTokens(),
                                                parsedUsage.outputTokens(), parsedUsage.totalTokens(),
                                                parsedUsage.providerRequestId(), true)
                                                : parsedUsage;
                                    }
                                    JsonNode delta = root.path("choices").path(0).path("delta");
                                    String token = delta.path("content").asText("");
                                    if (!token.isEmpty()) {
                                        emitted.set(true);
                                        answer.append(token);
                                        onToken.accept(token);
                                    }
                                }
                            } catch (java.io.IOException exception) {
                                throw new IllegalStateException("Failed to read model stream", exception);
                            }
                            return null;
                        });
                break;
            } catch (ResourceAccessException failure) {
                if (attempt >= 3 || emitted.get()) {
                    throw providerUnavailable();
                }
                try {
                    Thread.sleep(100L * (1L << (attempt - 1)));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw providerUnavailable();
                }
            } catch (RuntimeException failure) {
                throw providerUnavailable();
            }
        }
        return new ChatModelCallResult(answer.toString(), finalUsage[0], elapsed(started), "OK");
    }

    private static long elapsed(long started) {
        return Math.max(0, System.currentTimeMillis() - started);
    }

    private static BusinessException providerUnavailable() {
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "未配置或无法访问阿里百炼 AI 服务（请在 IDEA 环境变量或根目录 .env 设置 DASHSCOPE_API_KEY，见 docs/CONFIGURATION.md）");
    }

    static String quickSystemPrompt() {
        return QUICK_SYSTEM_PROMPT;
    }

    /** 返回可展示给管理员的简短原因，避免泄露密钥或完整响应体 */
    private static String safeFailureHint(Throwable failure) {
        if (failure == null) {
            return "未知原因";
        }
        String message = failure.getMessage();
        if (message != null && !message.isBlank()) {
            String trimmed = message.trim();
            if (trimmed.length() > 160) {
                trimmed = trimmed.substring(0, 160) + "…";
            }
            return trimmed;
        }
        return failure.getClass().getSimpleName();
    }

}
