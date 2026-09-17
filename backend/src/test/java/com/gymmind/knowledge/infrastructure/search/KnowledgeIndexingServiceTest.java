package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.knowledge.infrastructure.parser.DeterministicChunker;
import com.gymmind.knowledge.infrastructure.parser.DocumentParserRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeIndexingServiceTest {
    @Test
    void parsesChunksEmbedsAndPublishesToBothIndexes() {
        var index = mock(IndexGenerationService.class);
        var embedding = mock(EmbeddingGateway.class);
        when(embedding.embed(anyList())).thenReturn(List.of(new float[1024]));
        when(index.publish(eq(7L), startsWith("document-3-"), anyList())).thenReturn(true);
        var service = new KnowledgeIndexingService(index, embedding,
                DocumentParserRegistry.standard(), new DeterministicChunker(1200, 180), null);

        assertThat(service.publish(7L, 3L, 11L, "guide.txt", "text/plain", "training tips".getBytes())).isTrue();
        verify(embedding).embed(argThat(xs -> xs.size() == 1 && xs.get(0).contains("training tips")));
        verify(index).publish(eq(7L), startsWith("document-3-"),
                argThat(xs -> xs.size() == 1 && xs.get(0).vector().length == 1024));
    }
}
