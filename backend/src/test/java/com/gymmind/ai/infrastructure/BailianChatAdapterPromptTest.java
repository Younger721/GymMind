package com.gymmind.ai.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BailianChatAdapterPromptTest {
    @Test
    void quickPromptRequiresConciseAnswerWithoutReasoning() {
        String prompt = BailianChatAdapter.quickSystemPrompt();

        assertThat(prompt).contains("直接回答");
        assertThat(prompt).contains("不要输出分析过程");
        assertThat(prompt).contains("最多 2 条");
        assertThat(prompt).contains("1～3 句话");
        assertThat(prompt).contains("120 个中文字符");
    }
}
