package com.gymmind.ai.harness.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymmind.iam.domain.model.RoleCode;

import java.util.Set;

public record CapabilityDescriptor(
        String id,
        String name,
        String description,
        String type,
        String permission,
        boolean requiresTenant,
        Set<RoleCode> allowedRoles,
        JsonNode parameterSchema) {
    public CapabilityDescriptor {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Capability id is required");
        name = name == null ? id : name;
        description = description == null ? "" : description;
        type = type == null || type.isBlank() ? "READ" : type;
        allowedRoles = allowedRoles == null ? Set.of() : Set.copyOf(allowedRoles);
        parameterSchema = parameterSchema == null
                ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                : parameterSchema;
    }
}
