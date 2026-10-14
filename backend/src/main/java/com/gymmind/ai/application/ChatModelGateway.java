package com.gymmind.ai.application;

import com.gymmind.ai.domain.ContextSegment;

import java.util.List;
import java.util.function.Consumer;

public interface ChatModelGateway {
    String complete(String prompt, List<ContextSegment> context);

    default ChatModelCallResult completeWithUsage(String prompt, List<ContextSegment> context) {
        long started = System.currentTimeMillis();
        try {
            return ChatModelCallResult.unavailable(complete(prompt, context),
                    Math.max(0, System.currentTimeMillis() - started), "OK");
        } catch (RuntimeException exception) {
            return ChatModelCallResult.unavailable(null,
                    Math.max(0, System.currentTimeMillis() - started), "ERROR");
        }
    }

    /**
     * 结构化 JSON 任务（如训练计划草案）：需要更长输出，默认实现退化为普通 complete。
     */
    default ChatModelCallResult completeStructuredJson(String prompt, List<ContextSegment> context) {
        return completeWithUsage(prompt, context);
    }

    /** Whether this gateway can send native OpenAI-compatible tool definitions. */
    default boolean supportsToolCalling() {
        return false;
    }

    /** First model pass: direct answer or structured tool calls. */
    default ChatModelResponse completeWithTools(String prompt, List<ContextSegment> context,
                                                List<ChatToolDefinition> tools) {
        return ChatModelResponse.direct(complete(prompt, context));
    }

    default String stream(String prompt, List<ContextSegment> context, Consumer<String> onToken) {
        String answer = complete(prompt, context);
        if (answer != null && !answer.isEmpty()) {
            for (int i = 0; i < answer.length(); i += 12) {
                onToken.accept(answer.substring(i, Math.min(answer.length(), i + 12)));
            }
        }
        return answer;
    }

    default ChatModelCallResult streamWithUsage(String prompt, List<ContextSegment> context,
                                                Consumer<String> onToken) {
        long started = System.currentTimeMillis();
        try {
            return ChatModelCallResult.unavailable(stream(prompt, context, onToken),
                    Math.max(0, System.currentTimeMillis() - started), "OK");
        } catch (RuntimeException exception) {
            return ChatModelCallResult.unavailable(null,
                    Math.max(0, System.currentTimeMillis() - started), "ERROR");
        }
    }
}
