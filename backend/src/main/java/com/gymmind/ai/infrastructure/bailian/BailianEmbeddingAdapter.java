package com.gymmind.ai.infrastructure.bailian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.infrastructure.BailianCallRetrier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BailianEmbeddingAdapter implements EmbeddingGateway {

    private static final int MAX_BATCH_SIZE = 20;

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String model;
    private final boolean localFallback;
    private final BailianCallRetrier retrier;

    public BailianEmbeddingAdapter(RestClient.Builder builder, DashScopeProperties dashScope) {
        String key = dashScope.apiKeyForDashScopeOnly();
        this.localFallback = key.isBlank();
        this.model = dashScope.getEmbeddingModel();
        // 不在此处覆盖 RequestFactory，便于单测 MockRestServiceServer 拦截，超时由 RestClient.Builder Bean 统一配置
        this.client = builder.baseUrl(dashScope.baseUrlForDashScope())
                .defaultHeader("Authorization", "Bearer " + key)
                .build();
        this.retrier = new BailianCallRetrier(3, delayMs -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("embedding retry interrupted", interrupted);
            }
        });
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
        return retrier.execute(() -> parseEmbeddingResponse(texts, invokeEmbedding(texts)));
    }

    private String invokeEmbedding(List<String> texts) {
        Map<String, Object> body = Map.of("model", model, "input", texts);
        return client.post().uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private List<float[]> parseEmbeddingResponse(List<String> texts, String raw) {
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
