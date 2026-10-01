package com.gymmind.ai.harness;

import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.application.DefaultAiHarnessService;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultAiHarnessProgressTest {

    @Test
    void wrapsPluginExecutionWithStartedAndCompletedEvents() {
        AiHarnessPlugin plugin = new AiHarnessPlugin() {
            private final AiHarnessPluginDescriptor descriptor = new AiHarnessPluginDescriptor(
                    "knowledge_search", "知识库检索", "", "RAG", null, false, true);

            @Override
            public AiHarnessPluginDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
                context.add(new ContextSegment("chunk-1", "1", "依据"));
            }
        };
        var tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(TenantHarnessConfig.defaults(
                7L, "basic_chat,knowledge_search", "助手")));
        var model = mock(ChatModelGateway.class);
        when(model.stream(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any())).thenReturn("answer");
        var events = new ArrayList<AiProgressEvent>();
        var service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(plugin)), tenants,
                mock(com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository.class),
                new AiHarnessProperties(), model, null, null);

        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat"), 0, "t"),
                "session", "问题", List.of("basic_chat", "knowledge_search"), List.of(),
                ignored -> { }, events::add);

        assertThat(events).extracting(AiProgressEvent::stage)
                .containsExactly("plugin_start", "plugin_complete");
        assertThat(events.get(0).status()).isEqualTo(AiProgressEvent.Status.STARTED);
        assertThat(events.get(1).status()).isEqualTo(AiProgressEvent.Status.COMPLETED);
    }
}
