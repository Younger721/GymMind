package com.gymmind.agent.application;

import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface AgentManagementService {
    List<AgentView> list(CurrentActor actor);

    AgentView find(CurrentActor actor, Long agentId);

    AgentView create(CurrentActor actor, CreateAgentCommand command);

    AgentView update(CurrentActor actor, Long agentId, UpdateAgentCommand command);

    AgentView updatePrompt(CurrentActor actor, Long agentId, String systemPrompt);

    /** 更新 agent 启用的技能清单（内置工具名 + mcp:&lt;server&gt;:&lt;tool&gt; 远程技能） */
    AgentView updateTools(CurrentActor actor, Long agentId, java.util.List<String> enabledTools);

    AgentView disable(CurrentActor actor, Long agentId);

    AgentView activate(CurrentActor actor, Long agentId);
}
