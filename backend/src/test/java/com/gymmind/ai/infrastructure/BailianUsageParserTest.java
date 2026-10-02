package com.gymmind.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BailianUsageParserTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesOpenAiCompatibleUsageAndRequestId() throws Exception {
        var root = mapper.readTree("""
                {
                  "id": "chatcmpl-123",
                  "model": "qwen3.7-plus",
                  "usage": {"prompt_tokens": 120, "completion_tokens": 45, "total_tokens": 165}
                }
                """);

        var usage = BailianUsageParser.parse(root);

        assertThat(usage.inputTokens()).isEqualTo(120);
        assertThat(usage.outputTokens()).isEqualTo(45);
        assertThat(usage.totalTokens()).isEqualTo(165);
        assertThat(usage.providerRequestId()).isEqualTo("chatcmpl-123");
        assertThat(usage.model()).isEqualTo("qwen3.7-plus");
        assertThat(usage.available()).isTrue();
    }

    @Test
    void marksMissingUsageAsUnavailableInsteadOfZero() throws Exception {
        var usage = BailianUsageParser.parse(mapper.readTree("{\"id\":\"req-1\"}"));

        assertThat(usage.available()).isFalse();
        assertThat(usage.inputTokens()).isNull();
        assertThat(usage.outputTokens()).isNull();
        assertThat(usage.totalTokens()).isNull();
    }
}
