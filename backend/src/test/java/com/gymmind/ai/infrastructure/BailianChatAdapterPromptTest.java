package com.gymmind.ai.infrastructure;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void missingApiKeyDoesNotReturnLocalAnswer() {
        BailianChatAdapter adapter = new BailianChatAdapter(RestClient.builder(), "", "qwen3.7-plus");

        assertThat(adapter.supportsToolCalling()).isTrue();
        assertThatThrownBy(() -> adapter.completeWithTools("查训练记录", java.util.List.of(), java.util.List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE);
        assertThatThrownBy(() -> adapter.completeWithUsage("回答问题", java.util.List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE);
        assertThatThrownBy(() -> adapter.streamWithUsage("回答问题", java.util.List.of(), ignored -> { }))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE);
    }
}
