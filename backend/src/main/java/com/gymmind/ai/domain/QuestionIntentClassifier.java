package com.gymmind.ai.domain;

import java.util.Locale;

/** Deterministic first-pass intent classifier; model-based classification can be layered later. */
public final class QuestionIntentClassifier {
    public QuestionIntent classify(String question) {
        if (question == null || question.isBlank()) return QuestionIntent.GENERAL;
        String q = question.toLowerCase(Locale.ROOT);
        if (containsAny(q, "进度", "体重", "体脂", "测量", "记录")) return QuestionIntent.PROGRESS;
        if (containsAny(q, "营养", "饮食", "热量", "蛋白质", "碳水")) return QuestionIntent.NUTRITION;
        if (containsAny(q, "训练", "锻炼", "动作", "计划")) return QuestionIntent.TRAINING;
        return QuestionIntent.GENERAL;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) if (value.contains(keyword)) return true;
        return false;
    }
}
