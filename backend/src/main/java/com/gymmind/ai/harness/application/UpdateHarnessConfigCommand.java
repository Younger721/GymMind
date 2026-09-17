package com.gymmind.ai.harness.application;

import java.util.List;

public record UpdateHarnessConfigCommand(List<String> enabledPlugins, String systemPrompt) {
}
