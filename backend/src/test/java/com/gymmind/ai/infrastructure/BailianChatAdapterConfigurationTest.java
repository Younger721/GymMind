package com.gymmind.ai.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class BailianChatAdapterConfigurationTest {
    @Test
    void usesGymMindDesignDefaultChatModel() throws Exception {
        BailianChatAdapter adapter = new BailianChatAdapter(RestClient.builder());
        Field model = BailianChatAdapter.class.getDeclaredField("model");
        model.setAccessible(true);
        assertThat(model.get(adapter)).isEqualTo("qwen3.7-plus");
    }
}
