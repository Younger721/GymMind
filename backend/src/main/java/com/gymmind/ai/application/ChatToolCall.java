package com.gymmind.ai.application;

import com.fasterxml.jackson.databind.JsonNode;

public record ChatToolCall(String name, JsonNode arguments) {
    public ChatToolCall {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Tool name is required");
        arguments = arguments == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : arguments;
    }
}
