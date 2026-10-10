package com.gymmind.ai.infrastructure.bailian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.RankedChunk;
import com.gymmind.ai.domain.RerankGateway;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BailianRerankAdapter implements RerankGateway {

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String model;

    public BailianRerankAdapter(RestClient.Builder builder, DashScopeProperties dashScope) {
        this.model = dashScope.getRerankModel();
        String key = dashScope.apiKeyForDashScopeOnly();
        this.client = builder.baseUrl(dashScope.baseUrlForDashScope())
                .defaultHeader("Authorization", "Bearer " + key)
                .build();
    }

    @Override
    public List<RankedChunk> rerank(String query, List<ContextSegment> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("query and candidates required");
        }
        List<String> docs = candidates.stream().map(ContextSegment::text).toList();
        String raw = client.post().uri("/rerank")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "query", query,
                        "documents", docs,
                        "top_n", Math.min(8, docs.size())))
                .retrieve()
                .body(String.class);
        try {
            List<RankedChunk> out = new ArrayList<>();
            for (JsonNode node : mapper.readTree(raw).path("results")) {
                out.add(new RankedChunk(
                        candidates.get(node.path("index").asInt()).id(),
                        node.path("index").asInt() + 1));
            }
            return out;
        } catch (Exception exception) {
            throw new IllegalStateException("invalid rerank response", exception);
        }
    }
}
