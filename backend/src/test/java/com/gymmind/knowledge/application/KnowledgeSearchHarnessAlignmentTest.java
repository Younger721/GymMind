package com.gymmind.knowledge.application;

import com.gymmind.ai.application.AiChatView;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.application.DefaultAiHarnessService;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository;
import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import com.gymmind.ai.harness.plugins.KnowledgeSearchHarnessPlugin;
import com.gymmind.ai.harness.plugins.BasicChatHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginRegistry;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchHarnessAlignmentTest {

    @Test
    void harnessCitationChunkIdsMatchKnowledgeSearchServiceOrder() {
        KnowledgeSearchService search = mock(KnowledgeSearchService.class);
        List<ContextSegment> segments = List.of(
                new ContextSegment("chunk-a", "11", "alpha"),
                new ContextSegment("chunk-b", "12", "beta"));
        when(search.search(any(), anyString())).thenReturn(segments);
        when(search.search(any(), anyString(), any())).thenReturn(segments);

        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.streamWithUsage(anyString(), anyList(), any())).thenReturn(
                com.gymmind.ai.application.ChatModelCallResult.unavailable("ok", 1, "OK"));

        DefaultAiHarnessService harness = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(
                        new BasicChatHarnessPlugin(),
                        new KnowledgeSearchHarnessPlugin(search))),
                tenants,
                mock(PlatformHarnessConfigRepository.class),
                new AiHarnessProperties(),
                model,
                null,
                null);

        AiChatView view = harness.chat(
                new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat", "knowledge:read"), 0, "t"),
                "session", "问题", List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });

        assertThat(KnowledgeSearchCitations.chunkIdsInOrder(segments))
                .containsExactly("chunk-a", "chunk-b");
        assertThat(view.citations()).extracting(c -> c.chunkId())
                .containsExactly("chunk-a", "chunk-b");
        assertThat(view.sources()).containsExactly("chunk-a", "chunk-b");
    }
}
