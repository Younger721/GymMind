package com.gymmind.ai.harness.capability;

import java.util.UUID;
import java.util.function.Consumer;

import com.gymmind.ai.harness.AiProgressEvent;

public record ToolExecutionContext(String traceId, String question,
                                   Consumer<AiProgressEvent> progress) {
    public ToolExecutionContext {
        traceId = traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
        progress = progress == null ? ignored -> { } : progress;
    }
}
