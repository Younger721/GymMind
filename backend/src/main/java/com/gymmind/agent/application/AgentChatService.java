package com.gymmind.agent.application;

import com.gymmind.ai.application.AiChatView;
import com.gymmind.shared.security.CurrentActor;

public interface AgentChatService {
    AiChatView chat(CurrentActor actor, Long agentId, String sessionId, String question);
}
