package com.gymmind.ai.domain;
import java.util.Locale;
public final class PromptInjectionGuard {
    private static final String[] MARKERS = {"忽略之前", "忽略所有指令", "系统提示", "system prompt", "ignore previous", "ignore all instructions"};
    public boolean isBlocked(String question) {
        if (question == null || question.isBlank()) return false;
        String normalized = question.toLowerCase(Locale.ROOT);
        for (String marker : MARKERS) if (normalized.contains(marker)) return true;
        return false;
    }
}
