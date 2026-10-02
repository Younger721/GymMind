package com.gymmind.ai.application;

import com.fasterxml.jackson.databind.JsonNode;

/** OpenAI-compatible read-only tool definition exposed to the model. */
public record ChatToolDefinition(String name, String description, JsonNode parameters) {
    public ChatToolDefinition {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Tool name is required");
        description = description == null ? "" : description;
        parameters = parameters == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : parameters;
    }
}
