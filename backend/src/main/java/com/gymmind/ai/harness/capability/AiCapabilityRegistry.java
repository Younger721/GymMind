package com.gymmind.ai.harness.capability;

import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginRegistry;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AiCapabilityRegistry {
    private final Map<String, AiCapability> capabilities = new LinkedHashMap<>();

    public AiCapabilityRegistry(List<AiCapability> discovered) {
        for (AiCapability capability : discovered) {
            capabilities.put(capability.descriptor().id(), capability);
        }
    }

    @Autowired
    public AiCapabilityRegistry(AiHarnessPluginRegistry plugins, List<AiCapability> discovered) {
        this(discovered);
        for (AiHarnessPlugin plugin : plugins.all()) {
            capabilities.putIfAbsent(plugin.descriptor().id(), new LegacyAiHarnessCapabilityAdapter(plugin));
        }
    }

    public Optional<AiCapability> find(String id) { return Optional.ofNullable(capabilities.get(id)); }
    public Collection<AiCapability> all() { return capabilities.values(); }
    public List<AiCapability> listFor(CurrentActor actor, Collection<String> enabledIds) {
        return capabilities.values().stream()
                .filter(capability -> enabledIds == null || enabledIds.isEmpty() || enabledIds.contains(capability.descriptor().id()))
                .filter(capability -> capability.supports(actor))
                .toList();
    }
}
