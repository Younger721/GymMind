package com.gymmind.shared.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvPropertySupportTest {

    @Test
    void parseEnvLinesSkipsCommentsAndBlankValues() {
        Map<String, String> parsed = DotEnvPropertySupport.parseEnvLines(List.of(
                "# comment",
                "DASHSCOPE_API_KEY=sk-abc",
                "EMPTY=",
                "DASHSCOPE_BASE_URL=\"https://example.com/v1\""));
        assertThat(parsed).containsEntry("DASHSCOPE_API_KEY", "sk-abc");
        assertThat(parsed).containsEntry("DASHSCOPE_BASE_URL", "https://example.com/v1");
        assertThat(parsed).containsEntry("EMPTY", "");
    }
}
