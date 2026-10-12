package com.gymmind.ai.application;

/**
 * AI 训练计划生成请求（自由文本 + 结构化字段）。
 */
public record AiWorkoutPlanDraftRequest(
        String request,
        boolean includeHistory,
        String goal,
        Integer sessionsPerWeek,
        String experience) {

    public boolean hasStructuredInput() {
        return (goal != null && !goal.isBlank())
                || (sessionsPerWeek != null && sessionsPerWeek > 0)
                || (experience != null && !experience.isBlank());
    }

    public boolean hasAnyInput() {
        return (request != null && !request.isBlank()) || hasStructuredInput();
    }
}
