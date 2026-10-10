package com.gymmind.knowledge.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIngestJobPropertiesTest {

    @Test
    void retryDelayUsesExponentialBackoff() {
        var props = new KnowledgeIngestJobProperties(5, 30, 5);
        assertThat(props.retryDelaySeconds(1)).isEqualTo(30);
        assertThat(props.retryDelaySeconds(2)).isEqualTo(60);
        assertThat(props.leaseDuration().toMinutes()).isEqualTo(5);
    }
}
