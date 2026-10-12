package com.gymmind.ai.application;

import com.gymmind.workout.application.WorkoutHistorySummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutPlanPromptComposerTest {

    @Test
    void includesStructuredFieldsAndHistory() {
        String prompt = WorkoutPlanPromptComposer.compose(
                "左肩旧伤",
                "增肌",
                3,
                "新手",
                true,
                new WorkoutHistorySummary(2, "近两周完成 2 次训练。"));
        assertThat(prompt).contains("健身目标：增肌");
        assertThat(prompt).contains("每周训练次数：3");
        assertThat(prompt).contains("训练经验：新手");
        assertThat(prompt).contains("左肩旧伤");
        assertThat(prompt).contains("近两周完成 2 次训练");
    }
}
