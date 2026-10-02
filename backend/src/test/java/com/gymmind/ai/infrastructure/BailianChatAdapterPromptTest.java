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

    @Test
    void quickPromptPrefersDatabaseToolsForCurrentOperationalFacts() {
        assertThat(BailianChatAdapter.quickSystemPrompt())
                .contains("涉及签到、预约、会籍、训练记录或门店经营数据时")
                .contains("优先调用提供的只读工具");
    }

    @Test
    void localFallbackDoesNotEchoInternalPrompt() {
        String answer = BailianChatAdapter.localFallback();

        assertThat(answer).doesNotContain("当前身份规则");
        assertThat(answer).doesNotContain("纯AI回答规则");
        assertThat(answer).contains("DASHSCOPE_API_KEY");
    }
}
