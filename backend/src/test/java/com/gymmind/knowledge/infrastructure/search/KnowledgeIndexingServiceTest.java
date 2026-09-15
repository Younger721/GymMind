package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeIndexingServiceTest {
    @Test
    void parsesChunksEmbedsAndPublishesToBothIndexes() {
        var index = mock(IndexGenerationService.class);
        var embedding = mock(EmbeddingGateway.class);
        when(embedding.embed(anyList())).thenReturn(List.of(new float[1024]));
        when(index.publish(eq(7L), eq("document-3"), anyList())).thenReturn(true);
        var service = new KnowledgeIndexingService(index, embedding);

        assertThat(service.publish(7L, 3L, 11L, "guide.txt", "text/plain", "training tips".getBytes())).isTrue();
        verify(embedding).embed(argThat(xs -> xs.size() == 1 && xs.get(0).contains("training tips")));
        verify(index).publish(eq(7L), eq("document-3"), argThat(xs -> xs.size() == 1 && xs.get(0).vector().length == 1024));
    }
}
