package com.gymmind.ai.harness.application;

import com.gymmind.ai.application.AiChatView;
import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface AiHarnessService {

    HarnessConfigView getConfig(CurrentActor actor);

    HarnessConfigView updateConfig(CurrentActor actor, UpdateHarnessConfigCommand command);

    HarnessConfigView getPlatformConfig(CurrentActor actor);

    HarnessConfigView updatePlatformConfig(CurrentActor actor, UpdateHarnessConfigCommand command);

    AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> sessionPlugins);
}
