package com.gymmind.ai.infrastructure.bailian;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashScopePropertiesTest {

    @Test
    void usesDashScopeDefaultBaseUrl() {
        DashScopeProperties props = new DashScopeProperties();
        props.setApiKey("sk-test");
        assertThat(props.baseUrlForChat()).isEqualTo(DashScopeProperties.DEFAULT_BASE_URL);
        assertThat(props.baseUrlForDashScope()).isEqualTo(DashScopeProperties.DEFAULT_BASE_URL);
    }

    @Test
    void apiKeyForChatMatchesDashScopeKey() {
        DashScopeProperties props = new DashScopeProperties();
        props.setApiKey("sk-test");
        assertThat(props.apiKeyForChat()).isEqualTo("sk-test");
        assertThat(props.apiKeyForDashScopeOnly()).isEqualTo("sk-test");
    }

    @Test
    void dashScopeBaseUrlDefaultsWhenBlank() {
        DashScopeProperties props = new DashScopeProperties();
        props.setBaseUrl("");
        assertThat(props.baseUrlForDashScope()).isEqualTo(DashScopeProperties.DEFAULT_BASE_URL);
    }
}
