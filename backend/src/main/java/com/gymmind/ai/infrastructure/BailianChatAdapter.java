package com.gymmind.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.application.ChatModelResponse;
import com.gymmind.ai.application.ChatToolCall;
import com.gymmind.ai.application.ChatToolDefinition;
import com.gymmind.ai.domain.ContextSegment;
import org.springframework.stereotype.Component;
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

/** Bailian OpenAI-compatible chat adapter with a deterministic local fallback. */
@Component
public class BailianChatAdapter implements ChatModelGateway {
    private static final String QUICK_SYSTEM_PROMPT = "你是 GymMind AI。必须严格遵守用户请求中标记为【当前身份规则】的角色边界，不得自行把平台管理员、门店管理员或教练当成会员。直接回答，不要输出分析过程、思考步骤、检查过程或草稿；普通问题限制为 1～3 句话，总长度控制在 120 个中文字符以内。先给结论，再补充最多 2 条必要说明。信息不足时简短说明依据不足；只有纯 AI 模式且用户明确要求个性化方案时，才提出必要问题。若用户消息包含【RAG回答规则】，必须直接依据参考资料回答，不要向用户追问。不得编造。";
    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public BailianChatAdapter(RestClient.Builder builder) {
        String baseUrl = firstEnv("DASHSCOPE_BASE_URL", "AI_API_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = firstEnv("DASHSCOPE_API_KEY", "AI_API_KEY", "");
        this.model = env("DASHSCOPE_CHAT_MODEL", "qwen3.7-plus");
    }

    @Override
    public String complete(String prompt, List<ContextSegment> context) {
        if (apiKey.isBlank()) {
            return localFallback();
        }
        String contextText = context.stream().map(ContextSegment::text).reduce("", (a, b) -> a + "\n" + b);
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 256,
                "enable_thinking", false,
                "messages", List.of(
                Map.of("role", "system", "content", quickSystemPrompt()),
                        Map.of("role", "user", "content", prompt + "\n参考资料：" + contextText)));
        String raw = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(body).retrieve().body(String.class);
        try {
            return mapper.readTree(raw).path("choices").path(0).path("message").path("content").asText(raw);
        } catch (Exception ignored) {
            return raw == null ? "模型暂不可用" : raw;
        }
    }

    @Override
    public boolean supportsToolCalling() {
        return !apiKey.isBlank();
    }

    @Override
    public ChatModelResponse completeWithTools(String prompt, List<ContextSegment> context,
                                               List<ChatToolDefinition> tools) {
        if (apiKey.isBlank()) {
            return ChatModelResponse.direct(localFallback());
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
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools.stream().map(tool -> Map.of(
                    "type", "function",
                    "function", Map.of("name", tool.name(), "description", tool.description(), "parameters", tool.parameters())))
                    .toList());
        }
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
            return calls.isEmpty() ? ChatModelResponse.direct(content) : ChatModelResponse.toolCalls(calls);
        } catch (RuntimeException | java.io.IOException failure) {
            throw new IllegalStateException("模型工具调用暂时不可用", failure);
        }
    }

    @Override
    public String stream(String prompt, List<ContextSegment> context, Consumer<String> onToken) {
        if (apiKey.isBlank()) {
            String answer = localFallback();
            for (int i = 0; i < answer.length(); i += 12) {
                onToken.accept(answer.substring(i, Math.min(answer.length(), i + 12)));
            }
            return answer;
        }
        String contextText = context.stream().map(ContextSegment::text).reduce("", (a, b) -> a + "\n" + b);
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", true,
                "max_tokens", 256,
                "enable_thinking", false,
                "messages", List.of(
                Map.of("role", "system", "content", quickSystemPrompt()),
                        Map.of("role", "user", "content", prompt + "\n参考资料：" + contextText)));
        StringBuilder answer = new StringBuilder();
        AtomicBoolean emitted = new AtomicBoolean();
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
                if (attempt >= 3 || emitted.get()) throw failure;
                try {
                    Thread.sleep(100L * (1L << (attempt - 1)));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Model stream retry interrupted", interrupted);
                }
            }
        }
        return answer.toString();
    }

    static String localFallback() {
        return "当前未配置 AI 模型密钥，暂时无法生成回答。请配置 DASHSCOPE_API_KEY 后重试。";
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String firstEnv(String primary, String secondary, String fallback) {
        String value = env(primary, "");
        return value.isBlank() ? env(secondary, fallback) : value;
    }

    static String quickSystemPrompt() {
        return QUICK_SYSTEM_PROMPT;
    }

}
