package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;

import java.util.Arrays;
import java.util.List;

public record AgentView(
        Long id,
        Long tenantId,
        String name,
        String description,
        String systemPrompt,
        AgentStatus status,
        boolean knowledgeEnabled,
        List<String> enabledTools,
        Long createdByUserId) {

    public static AgentView from(TenantAgent agent) {
        List<String> tools = agent.getEnabledTools() == null || agent.getEnabledTools().isBlank()
                ? List.of()
                : Arrays.stream(agent.getEnabledTools().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return new AgentView(
                agent.getId(),
                agent.getTenantId(),
                agent.getName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                agent.getStatus(),
                agent.isKnowledgeEnabled(),
                tools,
                agent.getCreatedByUserId());
    }
}
