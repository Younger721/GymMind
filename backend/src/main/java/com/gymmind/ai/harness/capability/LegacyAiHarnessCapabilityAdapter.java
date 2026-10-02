package com.gymmind.ai.harness.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.shared.security.CurrentActor;

import java.util.ArrayList;
import java.util.List;

/** Keeps existing Harness plugins available through the capability protocol. */
public final class LegacyAiHarnessCapabilityAdapter implements AiCapability {
    private final AiHarnessPlugin plugin;
    private final CapabilityDescriptor descriptor;

    public LegacyAiHarnessCapabilityAdapter(AiHarnessPlugin plugin) {
        this.plugin = plugin;
        AiHarnessPluginDescriptor d = plugin.descriptor();
        this.descriptor = new CapabilityDescriptor(d.id(), d.name(), d.description(),
                "import_article".equals(d.id()) ? "WRITE" : "READ",
                d.permission(), d.requiresTenant(), java.util.Set.of(),
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
    }

    @Override public CapabilityDescriptor descriptor() { return descriptor; }
    @Override public boolean supports(CurrentActor actor) { return plugin.supports(actor); }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        List<ContextSegment> segments = new ArrayList<>();
        plugin.contribute(actor, context.question(), segments, context.progress());
        String text = segments.stream().map(ContextSegment::text).reduce("", (a, b) -> a + "\n" + b).trim();
        return new ToolResult(true, text, segments);
    }
}
