package com.gymmind.ai.application;

import com.gymmind.workout.application.WorkoutHistorySummary;

/**
 * 将结构化训练需求与历史摘要组合为模型 Prompt。
 */
public final class WorkoutPlanPromptComposer {

    private WorkoutPlanPromptComposer() {
    }

    public static String compose(
            String freeTextRequest,
            String goal,
            Integer sessionsPerWeek,
            String experience,
            boolean includeHistory,
            WorkoutHistorySummary summary) {
        StringBuilder prompt = new StringBuilder(
                "请仅输出结构化训练计划JSON，不要输出解释。"
                        + "每个动作使用 exerciseName 文本名称，不需要 exerciseId；"
                        + "包含 name、goal、startDate、endDate、description 与 items（dayOfWeek、phase、sortOrder、sets、reps、restSeconds、weight、notes）。");
        if (goal != null && !goal.isBlank()) {
            prompt.append("\n健身目标：").append(goal.trim());
        }
        if (sessionsPerWeek != null && sessionsPerWeek > 0) {
            prompt.append("\n每周训练次数：").append(sessionsPerWeek);
        }
        if (experience != null && !experience.isBlank()) {
            prompt.append("\n训练经验：").append(experience.trim());
        }
        if (freeTextRequest != null && !freeTextRequest.isBlank()) {
            prompt.append("\n补充说明：").append(freeTextRequest.trim());
        }
        prompt.append("\n训练历史：");
        if (!includeHistory) {
            prompt.append("未参考训练记录（用户已关闭）。");
        } else if (summary == null) {
            prompt.append("近期暂无可参考训练记录。");
        } else {
            prompt.append(summary.promptText());
        }
        return prompt.toString();
    }
}
