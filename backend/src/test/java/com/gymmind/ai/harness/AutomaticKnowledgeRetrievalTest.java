package com.gymmind.ai.harness;

import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.application.DefaultAiHarnessService;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository;
import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import com.gymmind.ai.harness.application.AiToolOrchestrationResult;
import com.gymmind.ai.harness.application.AiToolOrchestrator;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.AiCapabilityRegistry;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.plugins.BasicChatHarnessPlugin;
import com.gymmind.ai.application.ChatModelCallResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AutomaticKnowledgeRetrievalTest {

    @Test
    void disclosesMissingKnowledgeWhenKnowledgeOnlyRouteHasNoResults() {
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.stream(anyString(), anyList(), any()))
                .thenReturn("知识库未提供依据。以下内容仅供一般参考。");
        AiHarnessProperties properties = new AiHarnessProperties();
        properties.setRoutingMode(AiHarnessProperties.RoutingMode.LEGACY_ALWAYS_RAG);
        AiHarnessPlugin emptyKnowledge = new AiHarnessPlugin() {
            private final AiHarnessPluginDescriptor descriptor = new AiHarnessPluginDescriptor(
                    "knowledge_search", "知识库检索", "", "RAG", null, false, true);

            @Override
            public AiHarnessPluginDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
            }
        };
        DefaultAiHarnessService service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin(), emptyKnowledge)), tenants,
                mock(PlatformHarnessConfigRepository.class), properties, model, null, null);
        CurrentActor actor = new CurrentActor(
                1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "t");

        var result = service.chat(actor, "session", "运动损伤处理规范是什么？",
                List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(model).stream(prompt.capture(), anyList(), any());
        assertThat(prompt.getValue()).contains("没有找到与问题直接相关的知识库资料", "一般参考");
        assertThat(result.answer()).contains("知识库未提供依据。");
    }

    @Test
    void refusesWithoutCallingModelWhenKnowledgeOnlyRouteHasNoResultsAndPolicyIsRefuse() {
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        AiHarnessProperties properties = new AiHarnessProperties();
        properties.setRoutingMode(AiHarnessProperties.RoutingMode.LEGACY_ALWAYS_RAG);
        properties.setEmptyKnowledgePolicy(AiHarnessProperties.EmptyKnowledgePolicy.REFUSE);
        AiHarnessPlugin emptyKnowledge = new AiHarnessPlugin() {
            private final AiHarnessPluginDescriptor descriptor = new AiHarnessPluginDescriptor(
                    "knowledge_search", "知识库检索", "", "RAG", null, false, true);

            @Override
            public AiHarnessPluginDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
            }
        };
        DefaultAiHarnessService service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin(), emptyKnowledge)), tenants,
                mock(PlatformHarnessConfigRepository.class), properties, model, null, null);
        CurrentActor actor = new CurrentActor(
                1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "t");

        var result = service.chat(actor, "session", "运动损伤处理规范是什么？",
                List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });

        assertThat(result.answer()).isEqualTo("没有找到足够可靠的知识依据，暂不提供确定性建议。");
        verify(model, never()).streamWithUsage(anyString(), anyList(), any());
        verify(model, never()).stream(anyString(), anyList(), any());
    }

    @Test
    void automaticallyRetrievesKnowledgeWhenSessionDoesNotSelectRag() {
        AiHarnessPlugin plugin = knowledgePlugin("自动检索依据");
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.stream(anyString(), anyList(), any())).thenReturn("answer");
        List<AiProgressEvent> events = new ArrayList<>();
        DefaultAiHarnessService service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin(), plugin)), tenants,
                mock(PlatformHarnessConfigRepository.class), new AiHarnessProperties(), model, null, null);

        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat"), 0, "t"),
                "session", "问题", List.of("basic_chat"), List.of(), ignored -> { }, events::add);

        assertThat(events).extracting(AiProgressEvent::pluginId).contains("knowledge_search");
        ArgumentCaptor<List> context = ArgumentCaptor.forClass(List.class);
        verify(model).stream(anyString(), context.capture(), any());
        assertThat(context.getValue()).extracting("text").contains("自动检索依据");
    }

    @Test
    void passesAutomaticKnowledgeContextToNativeToolOrchestrator() {
        AiHarnessPlugin plugin = knowledgePlugin("原生工具前的知识依据");
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.supportsToolCalling()).thenReturn(true);
        when(model.streamWithUsage(anyString(), anyList(), any())).thenReturn(
                ChatModelCallResult.unavailable("answer", 0, "OK"));
        AiCapabilityRegistry capabilities = mock(AiCapabilityRegistry.class);
        AiCapability basic = capability("basic_chat");
        AiCapability knowledge = capability("knowledge_search");
        AiCapability metrics = capability("gym_operations_metrics");
        when(capabilities.listFor(any(), any())).thenReturn(List.of(basic, knowledge, metrics));
        AiToolOrchestrator tools = mock(AiToolOrchestrator.class);
        when(tools.decide(any(), anyString(), anyList(), anyList(), any(), any())).thenReturn(
                new AiToolOrchestrationResult(null, List.of(), List.of()));
        AiHarnessProperties properties = new AiHarnessProperties();
        properties.setRoutingMode(AiHarnessProperties.RoutingMode.LEGACY_ALWAYS_RAG);
        DefaultAiHarnessService service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin(), plugin)), tenants,
                mock(PlatformHarnessConfigRepository.class), properties, model, null, null,
                null, null, capabilities, tools);

        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat"), 0, "t"),
                "session", "运营问题", List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });

        ArgumentCaptor<List> context = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(tools).decide(any(), anyString(), context.capture(), anyList(), any(), any());
        assertThat(context.getValue()).extracting("text").contains("原生工具前的知识依据");
    }

    @Test
    void skipsKnowledgeWhenAutoRouteSelectsStoreMetrics() {
        AiHarnessPlugin plugin = knowledgePlugin("不应注入的片段");
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.supportsToolCalling()).thenReturn(true);
        when(model.streamWithUsage(anyString(), anyList(), any())).thenReturn(
                ChatModelCallResult.unavailable("answer", 0, "OK"));
        AiCapabilityRegistry capabilities = mock(AiCapabilityRegistry.class);
        AiCapability metrics = capability("gym_operations_metrics");
        when(capabilities.listFor(any(), any())).thenReturn(List.of(metrics));
        AiToolOrchestrator tools = mock(AiToolOrchestrator.class);
        when(tools.decide(any(), anyString(), anyList(), anyList(), any(), any())).thenReturn(
                new AiToolOrchestrationResult(null, List.of(), List.of()));
        DefaultAiHarnessService service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin(), plugin)), tenants,
                mock(PlatformHarnessConfigRepository.class), new AiHarnessProperties(), model, null, null,
                null, null, capabilities, tools);
        List<AiProgressEvent> events = new ArrayList<>();

        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat"), 0, "t"),
                "session", "上个月来了多少会员？", List.of("basic_chat"), List.of(), ignored -> { }, events::add);

        assertThat(events).extracting(AiProgressEvent::pluginId).doesNotContain("knowledge_search");
        assertThat(events).extracting(AiProgressEvent::pluginId).contains("answer_router");
    }

    @Test
    void doesNotExecuteKeywordToolsWhenNativeToolCallingIsUnavailable() {
        AiHarnessPlugin plugin = knowledgePlugin("自动检索依据");
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search,member_training_context", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.supportsToolCalling()).thenReturn(false);
        when(model.streamWithUsage(anyString(), anyList(), any())).thenReturn(
                ChatModelCallResult.unavailable("answer", 0, "OK"));
        AiCapabilityRegistry capabilities = mock(AiCapabilityRegistry.class);
        AiCapability basic = capability("basic_chat");
        AiCapability training = capability("member_training_context");
        when(capabilities.listFor(any(), any())).thenReturn(List.of(basic, training));
        when(capabilities.find("member_training_context")).thenReturn(Optional.of(training));
        AiToolOrchestrator tools = mock(AiToolOrchestrator.class);
        DefaultAiHarnessService service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin(), plugin)), tenants,
                mock(PlatformHarnessConfigRepository.class), new AiHarnessProperties(), model, null, null,
                null, null, capabilities, tools);

        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "t"),
                "session", "我最近训练记录如何", List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });

        verifyNoInteractions(tools);
    }

    @Test
    void propagatesProviderFailureInsteadOfReturningSyntheticAnswer() {
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat", "助手")));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.streamWithUsage(anyString(), anyList(), any())).thenReturn(
                new ChatModelCallResult(null,
                        com.gymmind.ai.application.ModelUsage.unavailable("dashscope", "qwen3.7-plus", null),
                        5, "ERROR"));
        DefaultAiHarnessService service = new DefaultAiHarnessService(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin())), tenants,
                mock(PlatformHarnessConfigRepository.class), new AiHarnessProperties(), model, null, null);

        assertThatThrownBy(() -> service.chat(
                new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "t"),
                "session", "回答一个问题", List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { }))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE);
    }

    private static AiHarnessPlugin knowledgePlugin(String text) {
        return new AiHarnessPlugin() {
            private final AiHarnessPluginDescriptor descriptor = new AiHarnessPluginDescriptor(
                    "knowledge_search", "知识库检索", "", "RAG", null, false, true);

            @Override
            public AiHarnessPluginDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
                context.add(new ContextSegment("chunk-1", "doc-1", text));
            }
        };
    }

    private static AiCapability capability(String id) {
        AiCapability capability = mock(AiCapability.class);
        when(capability.descriptor()).thenReturn(new CapabilityDescriptor(
                id, id, id, "READ", null, false, Set.of(), null));
        when(capability.supports(any())).thenReturn(true);
        return capability;
    }
}
