package com.gymmind.agent.application;

import java.util.List;

public record UpdateAgentCommand(
        String name,
        String description,
        String systemPrompt,
        boolean knowledgeEnabled,
        List<String> enabledTools) {
}
