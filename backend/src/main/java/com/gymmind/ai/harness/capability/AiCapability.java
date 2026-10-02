package com.gymmind.ai.harness.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;

public interface AiCapability {
    CapabilityDescriptor descriptor();

    default boolean supports(CurrentActor actor) {
        if (actor == null) return false;
        CapabilityDescriptor d = descriptor();
        if (d.requiresTenant() && ActorAccess.tenantId(actor) == null) return false;
        if (!d.allowedRoles().isEmpty() && d.allowedRoles().stream().noneMatch(actor.roles()::contains)) return false;
        return d.permission() == null || d.permission().isBlank() || actor.hasPermission(d.permission());
    }

    ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context);
}
