package com.gymmind.ai.harness.application;

import com.gymmind.ai.application.AiChatView;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.shared.security.CurrentActor;

import java.util.List;
import java.util.function.Consumer;

public interface AiHarnessService {

    HarnessConfigView getConfig(CurrentActor actor);

    HarnessConfigView updateConfig(CurrentActor actor, UpdateHarnessConfigCommand command);

    HarnessConfigView getPlatformConfig(CurrentActor actor);

    HarnessConfigView updatePlatformConfig(CurrentActor actor, UpdateHarnessConfigCommand command);

    AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> sessionPlugins);

    default AiChatView chat(CurrentActor actor, String sessionId, String question,
                            List<String> sessionPlugins, List<AiChatTurn> history,
                            Consumer<String> onToken) {
        return chat(actor, sessionId, question, sessionPlugins, history, onToken, ignored -> { });
    }

    default AiChatView chat(CurrentActor actor, String sessionId, String question,
                            List<String> sessionPlugins, List<AiChatTurn> history,
                            Consumer<String> onToken, Consumer<AiProgressEvent> onProgress) {
        return chat(actor, sessionId, question, sessionPlugins, onToken);
    }

    default AiChatView chat(CurrentActor actor, String sessionId, String question, List<String> plugins,
                            Consumer<String> onToken) {
        AiChatView result = chat(actor, sessionId, question, plugins);
        String answer = result.answer() == null ? "" : result.answer();
        for (int i = 0; i < answer.length(); i += 12) {
            onToken.accept(answer.substring(i, Math.min(answer.length(), i + 12)));
        }
        return result;
    }
}
