package com.gymmind.agent.application;

import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface AgentManagementService {
    List<AgentView> list(CurrentActor actor);

    AgentView find(CurrentActor actor, Long agentId);

    AgentView create(CurrentActor actor, CreateAgentCommand command);

    AgentView update(CurrentActor actor, Long agentId, UpdateAgentCommand command);

    AgentView disable(CurrentActor actor, Long agentId);

    AgentView activate(CurrentActor actor, Long agentId);
}
