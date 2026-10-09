package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIndexingServiceWiringTest {
    @Test
    void productionConstructorIsTheSpringInjectionEntryPoint() throws Exception {
        var constructor = KnowledgeIndexingService.class.getConstructor(
                IndexGenerationService.class,
                EmbeddingGateway.class);
        assertThat(constructor).isNotNull();
        assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }
}
