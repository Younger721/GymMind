package com.gymmind.shared.async;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

class JobQueueSelectionTest {

    @Test
    void queueImplementationsAreSelectedByTheSameProperty() {
        ConditionalOnProperty memory = InMemoryPersistentJobQueue.class
                .getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty mysql = MySqlPersistentJobQueue.class
                .getAnnotation(ConditionalOnProperty.class);

        assertThat(memory.name()).containsExactly("gymmind.job-queue");
        assertThat(memory.havingValue()).isEqualTo("memory");
        assertThat(memory.matchIfMissing()).isTrue();
        assertThat(mysql.name()).containsExactly("gymmind.job-queue");
        assertThat(mysql.havingValue()).isEqualTo("mysql");
    }
}
