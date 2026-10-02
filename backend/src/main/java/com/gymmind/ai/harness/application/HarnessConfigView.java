package com.gymmind.ai.harness.application;

import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;

import java.util.List;

public record HarnessConfigView(
        List<String> enabledPlugins,
        List<AiHarnessPluginDescriptor> availablePlugins,
        String systemPrompt,
        boolean platformScope,
        List<CapabilityDescriptor> availableCapabilities) {
    public HarnessConfigView(List<String> enabledPlugins,
                             List<AiHarnessPluginDescriptor> availablePlugins,
                             String systemPrompt,
                             boolean platformScope) {
        this(enabledPlugins, availablePlugins, systemPrompt, platformScope, List.of());
    }
}
