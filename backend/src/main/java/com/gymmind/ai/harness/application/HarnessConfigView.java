package com.gymmind.ai.harness.application;

import com.gymmind.ai.harness.AiHarnessPluginDescriptor;

import java.util.List;

public record HarnessConfigView(
        List<String> enabledPlugins,
        List<AiHarnessPluginDescriptor> availablePlugins,
        String systemPrompt,
        boolean platformScope) {
}
