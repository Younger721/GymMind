package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIndexingServiceWiringTest {
    @Test
    void productionConstructorIsTheSpringInjectionEntryPoint() throws Exception {
        var constructor = KnowledgeIndexingService.class
                .getConstructor(IndexGenerationService.class, EmbeddingGateway.class);

        assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
    }
}
