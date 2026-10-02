package com.gymmind.ai.application;

import java.util.List;

/** First-pass model response. A response is either text or bounded tool calls. */
public record ChatModelResponse(String content, List<ChatToolCall> toolCalls, ModelUsage usage) {
    public ChatModelResponse(String content, List<ChatToolCall> toolCalls) {
        this(content, toolCalls, null);
    }
    public ChatModelResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static ChatModelResponse direct(String content) {
        return new ChatModelResponse(content, List.of(), null);
    }

    public static ChatModelResponse toolCalls(List<ChatToolCall> calls) {
        return new ChatModelResponse(null, calls, null);
    }

    public static ChatModelResponse direct(String content, ModelUsage usage) {
        return new ChatModelResponse(content, List.of(), usage);
    }

    public static ChatModelResponse toolCalls(List<ChatToolCall> calls, ModelUsage usage) {
        return new ChatModelResponse(null, calls, usage);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
