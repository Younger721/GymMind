package com.gymmind.ai.infrastructure.bailian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.domain.EmbeddingGateway;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class BailianEmbeddingAdapter implements EmbeddingGateway {

    private static final int MAX_BATCH_SIZE = 20;

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String model;
    private final boolean localFallback;

    public BailianEmbeddingAdapter(RestClient.Builder builder) {
        String key = System.getenv("DASHSCOPE_API_KEY");
        this.localFallback = key == null || key.isBlank();
        this.model = Optional.ofNullable(System.getenv("DASHSCOPE_EMBEDDING_MODEL")).orElse("qwen3.7-text-embedding");
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.client = builder.baseUrl(Optional.ofNullable(System.getenv("DASHSCOPE_BASE_URL"))
                        .orElse("https://dashscope.aliyuncs.com/compatible-mode/v1"))
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + Optional.ofNullable(key).orElse(""))
                .build();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be empty");
        }
        if (localFallback) {
            return texts.stream().map(BailianEmbeddingAdapter::deterministicVector).toList();
        }
        List<float[]> out = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += MAX_BATCH_SIZE) {
            int end = Math.min(start + MAX_BATCH_SIZE, texts.size());
            out.addAll(embedBatch(texts.subList(start, end)));
        }
        return out;
    }

    private List<float[]> embedBatch(List<String> texts) {
        // DashScope's OpenAI-compatible endpoint expects input to be a JSON array.
        Map<String, Object> body = Map.of("model", model, "input", texts);
        String raw = client.post().uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode data = mapper.readTree(raw).path("data");
            List<float[]> out = new ArrayList<>();
            for (JsonNode row : data) {
                JsonNode emb = row.path("embedding");
                float[] vector = new float[emb.size()];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = (float) emb.get(i).asDouble();
                }
                out.add(vector);
            }
            EmbeddingDimension.validate(out, texts.size());
            return out;
        } catch (Exception ex) {
            throw new IllegalStateException("invalid embedding response", ex);
        }
    }

    static float[] deterministicVector(String text) {
        float[] vector = new float[EmbeddingDimension.DIMENSIONS];
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            vector[i % vector.length] += (bytes[i] & 0xff) / 255f;
        }
        float norm = 0f;
        for (float value : vector) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0f) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
