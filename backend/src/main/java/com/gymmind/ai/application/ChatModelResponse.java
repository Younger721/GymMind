package com.gymmind.ai.application;

import java.util.List;

/** First-pass model response. A response is either text or bounded tool calls. */
public record ChatModelResponse(String content, List<ChatToolCall> toolCalls) {
    public ChatModelResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static ChatModelResponse direct(String content) {
        return new ChatModelResponse(content, List.of());
    }

    public static ChatModelResponse toolCalls(List<ChatToolCall> calls) {
        return new ChatModelResponse(null, calls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
