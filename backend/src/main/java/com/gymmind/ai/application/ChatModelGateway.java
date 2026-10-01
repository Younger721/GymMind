package com.gymmind.ai.application;

import com.gymmind.ai.domain.ContextSegment;

import java.util.List;
import java.util.function.Consumer;

public interface ChatModelGateway {
    String complete(String prompt, List<ContextSegment> context);

    default String stream(String prompt, List<ContextSegment> context, Consumer<String> onToken) {
        String answer = complete(prompt, context);
        if (answer != null && !answer.isEmpty()) {
            for (int i = 0; i < answer.length(); i += 12) {
                onToken.accept(answer.substring(i, Math.min(answer.length(), i + 12)));
            }
        }
        return answer;
    }
}
