package com.gymmind.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymmind.ai.application.ModelUsage;

/** Parses the usage portion of an OpenAI-compatible DashScope response. */
final class BailianUsageParser {
    private BailianUsageParser() {
    }

    static ModelUsage parse(JsonNode root) {
        String requestId = text(root, "id");
        String model = text(root, "model");
        JsonNode usage = root == null ? null : root.get("usage");
        if (usage == null || usage.isNull() || !usage.isObject()) {
            return ModelUsage.unavailable("dashscope", model, requestId);
        }
        Integer input = integer(usage, "prompt_tokens", "input_tokens");
        Integer output = integer(usage, "completion_tokens", "output_tokens");
        Integer total = integer(usage, "total_tokens");
        if (total == null && input != null && output != null) {
            total = input + output;
        }
        if (input == null || output == null || total == null) {
            return ModelUsage.unavailable("dashscope", model, requestId);
        }
        return new ModelUsage("dashscope", model, input, output, total, requestId, true);
    }

    private static Integer integer(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.canConvertToInt()) {
                return value.intValue();
            }
        }
        return null;
    }

    private static String text(JsonNode node, String name) {
        if (node == null) return null;
        JsonNode value = node.get(name);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
