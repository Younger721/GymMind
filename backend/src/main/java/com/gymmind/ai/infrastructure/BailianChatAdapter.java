package com.gymmind.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.domain.ContextSegment;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Bailian OpenAI-compatible chat adapter with a deterministic local fallback. */
@Component
public class BailianChatAdapter implements ChatModelGateway {
    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final BailianCallRetrier retrier;

    public BailianChatAdapter(RestClient.Builder builder) {
        String baseUrl = env("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = env("DASHSCOPE_API_KEY", "");
        this.model = env("DASHSCOPE_CHAT_MODEL", "qwen3.7-plus");
        this.retrier = new BailianCallRetrier(3, BailianChatAdapter::sleep);
    }

    @Override
    public String complete(String prompt, List<ContextSegment> context) {
        if (apiKey.isBlank()) {
            return "本地模式：已收到问题“" + prompt + "”，参考知识片段 " + context.size() + " 条。";
        }
        String contextText = context.stream().map(ContextSegment::text).reduce("", (a, b) -> a + "\n" + b);
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是GymMind智能健身助手，请基于参考资料回答。"),
                        Map.of("role", "user", "content", prompt + "\n参考资料：" + contextText)));
        String raw = retrier.execute(() -> client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(body).retrieve().body(String.class));
        try {
            JsonNode root = mapper.readTree(raw);
            return root.path("choices").path(0).path("message").path("content").asText(raw);
        } catch (Exception ignored) {
            return raw == null ? "模型暂不可用" : raw;
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bailian retry interrupted", interrupted);
        }
    }
}
