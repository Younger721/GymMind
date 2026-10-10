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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeIndexingServiceTest {
    @Test
    void parsesChunksEmbedsAndPublishesToBothIndexes() {
        var index = mock(IndexGenerationService.class);
        var embedding = mock(EmbeddingGateway.class);
        when(embedding.embed(anyList())).thenReturn(List.of(new float[1024]));
        when(index.publishWithRollback(eq(7L), anyList())).thenReturn(true);
        var service = new KnowledgeIndexingService(index, embedding,
                DocumentParserRegistry.standard(), new DeterministicChunker(1200, 180));

        var result = service.publishIndex(7L, 3L, 11L, "guide.txt", "text/plain", "training tips".getBytes());
        assertThat(result.success()).isTrue();
        assertThat(result.indexGeneration()).startsWith("document-3-");
        verify(embedding).embed(argThat(xs -> xs.size() == 1 && xs.get(0).contains("training tips")));
        verify(index).publishWithRollback(eq(7L), argThat(xs -> xs.size() == 1 && xs.get(0).vector().length == 1024
                && xs.get(0).indexGeneration().equals(result.indexGeneration())));
        verify(index).gcStaleIndexGenerations(7L, 3L, result.indexGeneration());
    }

    @Test
    void gcStaleGenerationsAfterSuccessfulPublishWithoutUpfrontDelete() {
        var index = mock(IndexGenerationService.class);
        var embedding = mock(EmbeddingGateway.class);
        when(embedding.embed(anyList())).thenReturn(List.of(new float[1024]));
        when(index.publishWithRollback(eq(7L), anyList())).thenReturn(true);

        var service = new KnowledgeIndexingService(index, embedding,
                DocumentParserRegistry.standard(), new DeterministicChunker(1200, 180));

        assertThat(service.publishIndex(7L, 3L, 11L, "guide.txt", "text/plain", "new text".getBytes()).success()).isTrue();

        verify(index).gcStaleIndexGenerations(eq(7L), eq(3L), argThat(gen -> gen.startsWith("document-3-")));
        org.mockito.Mockito.verify(index, org.mockito.Mockito.never()).deleteDocument(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }
}
