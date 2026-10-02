package com.gymmind.ai.harness.capability;

import com.gymmind.ai.domain.ContextSegment;

import java.util.List;

public record ToolResult(boolean success, String message, List<ContextSegment> context) {
    public ToolResult {
        message = message == null ? "" : message;
        context = context == null ? List.of() : List.copyOf(context);
    }

    public static ToolResult context(String text) {
        return new ToolResult(true, text, List.of(new ContextSegment(null, "TOOL_RESULT", text)));
    }

    public static ToolResult context(String id, String text) {
        return new ToolResult(true, text, List.of(new ContextSegment(id, "TOOL_RESULT", text)));
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, message, List.of(new ContextSegment(null, "TOOL_ERROR", message)));
    }
}
