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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import org.mockito.ArgumentCaptor;

class DefaultAiHarnessProgressTest {

    @Test
    void keepsLegacyProgressConstructorAndCarriesFinalDocumentDetails() {
        AiProgressEvent legacy = new AiProgressEvent("knowledge_search", "plugin_start",
                AiProgressEvent.Status.STARTED, "启动 RAG 插件", null);
        assertThat(legacy.details()).isEmpty();

        AiHarnessPlugin plugin = new AiHarnessPlugin() {
            private final AiHarnessPluginDescriptor descriptor = new AiHarnessPluginDescriptor(
                    "knowledge_search", "知识库检索", "", "RAG", null, false, true);

            @Override
            public AiHarnessPluginDescriptor descriptor() { return descriptor; }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
                context.add(new ContextSegment("chunk-1", "42", "最终采用的授权片段"));
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

        AiProgressEvent complete = events.stream().filter(event -> "plugin_complete".equals(event.stage())).findFirst().orElseThrow();
        assertThat(complete.details()).singleElement().satisfies(detail -> {
            assertThat(detail.kind()).isEqualTo("DOCUMENT");
            assertThat(detail.documentId()).isEqualTo("42");
            assertThat(detail.chunkId()).isEqualTo("chunk-1");
            assertThat(detail.excerpt()).isEqualTo("最终采用的授权片段");
        });
    }

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
                .containsSubsequence("plugin_start", "plugin_complete", "answer_generation");
        assertThat(events).filteredOn(event -> "knowledge_search".equals(event.pluginId()))
                .extracting(AiProgressEvent::status)
                .containsExactly(AiProgressEvent.Status.STARTED, AiProgressEvent.Status.COMPLETED);
    }

    @Test
    void reportsAnswerGenerationUntilModelFinishesStreaming() {
        var tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(TenantHarnessConfig.defaults(
                7L, "basic_chat", "助手")));
        var model = mock(ChatModelGateway.class);
        var events = new ArrayList<AiProgressEvent>();
        var service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of()), tenants,
                mock(com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository.class),
                new AiHarnessProperties(), model, null, null);
        doAnswer(invocation -> {
            assertThat(events).anySatisfy(event -> {
                assertThat(event.pluginId()).isEqualTo("answer_generator");
                assertThat(event.status()).isEqualTo(AiProgressEvent.Status.STARTED);
            });
            invocation.<java.util.function.Consumer<String>>getArgument(2).accept("回答");
            return com.gymmind.ai.application.ChatModelCallResult.unavailable("回答", 10, "OK");
        }).when(model).streamWithUsage(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());

        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat"), 0, "t"),
                "session", "问题", List.of("basic_chat"), List.of(), ignored -> { }, events::add);

        assertThat(events).extracting(AiProgressEvent::stage)
                .contains("answer_generation");
        assertThat(events).filteredOn(event -> "answer_generator".equals(event.pluginId()))
                .extracting(AiProgressEvent::status)
                .containsExactly(AiProgressEvent.Status.STARTED, AiProgressEvent.Status.COMPLETED);
    }

    @Test
    void ragPromptAnswersFromRetrievedContextInsteadOfAskingFollowUpQuestions() {
        AiHarnessPlugin plugin = new AiHarnessPlugin() {
            private final AiHarnessPluginDescriptor descriptor = new AiHarnessPluginDescriptor(
                    "knowledge_search", "知识库检索", "", "RAG", null, false, true);

            @Override
            public AiHarnessPluginDescriptor descriptor() { return descriptor; }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
                context.add(new ContextSegment("chunk-1", "1", "新手训练先热身，使用稳定重量。"));
            }
        };
        var tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(TenantHarnessConfig.defaults(
                7L, "basic_chat,knowledge_search", "助手")));
        var model = mock(ChatModelGateway.class);
        when(model.stream(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any())).thenReturn("直接回答");
        var service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(plugin)), tenants,
                mock(com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository.class),
                new AiHarnessProperties(), model, null, null);

        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat"), 0, "t"),
                "session", "新手怎么训练？", List.of("basic_chat", "knowledge_search"), List.of(),
                ignored -> { }, ignored -> { });

        var prompt = ArgumentCaptor.forClass(String.class);
        verify(model).stream(prompt.capture(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
        assertThat(prompt.getValue()).contains("已启用知识库检索");
        assertThat(prompt.getValue()).contains("必须直接根据参考资料回答");
        assertThat(prompt.getValue()).contains("不要向用户追问");
    }
}
