package com.gymmind.ai.harness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.application.ChatModelResponse;
import com.gymmind.ai.application.ChatToolCall;
import com.gymmind.ai.application.ChatToolDefinition;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.AiCapabilityRegistry;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.ai.harness.application.AiToolOrchestrator;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiToolOrchestratorTest {

    private static final CurrentActor ACTOR = new CurrentActor(11L, 7L,
            Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "test-token");

    @Test
    void directAnswerDoesNotExecuteCapabilities() {
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.completeWithTools(anyString(), anyList(), anyList()))
                .thenReturn(ChatModelResponse.direct("直接回答"));
        AiCapability capability = capability("member_training_context");
        AiToolOrchestrator orchestrator = new AiToolOrchestrator(
                new AiCapabilityRegistry(List.of(capability)), model, new ObjectMapper());

        var result = orchestrator.decide(ACTOR, "高位下拉怎么做？", List.of(), ignored -> { });

        assertThat(result.directAnswer()).isEqualTo("直接回答");
        assertThat(result.toolResults()).isEmpty();
        verify(capability, never()).execute(any(), any(), any());
    }

    @Test
    void executesAtMostTwoAllowedTools() throws Exception {
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.completeWithTools(anyString(), anyList(), anyList()))
                .thenReturn(ChatModelResponse.toolCalls(List.of(
                        new ChatToolCall("member_training_context", new ObjectMapper().createObjectNode()),
                        new ChatToolCall("training_plan_calendar", new ObjectMapper().createObjectNode()))));
        AiCapability first = capability("member_training_context");
        AiCapability second = capability("training_plan_calendar");
        when(first.execute(any(), any(), any())).thenReturn(ToolResult.context("训练记录"));
        when(second.execute(any(), any(), any())).thenReturn(ToolResult.context("训练日历"));
        AiToolOrchestrator orchestrator = new AiToolOrchestrator(
                new AiCapabilityRegistry(List.of(first, second)), model, new ObjectMapper());

        var result = orchestrator.decide(ACTOR, "我最近训练少了吗？", List.of(), ignored -> { });

        assertThat(result.directAnswer()).isNull();
        assertThat(result.toolResults()).hasSize(2);
        verify(first).execute(any(), any(), any());
        verify(second).execute(any(), any(), any());
    }

    @Test
    void rejectsUnknownToolCall() {
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.completeWithTools(anyString(), anyList(), anyList()))
                .thenReturn(ChatModelResponse.toolCalls(List.of(
                        new ChatToolCall("delete_everything", new ObjectMapper().createObjectNode()))));
        AiToolOrchestrator orchestrator = new AiToolOrchestrator(
                new AiCapabilityRegistry(List.of()), model, new ObjectMapper());

        assertThatThrownBy(() -> orchestrator.decide(ACTOR, "帮我查一下", List.of(), ignored -> { }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持");
    }

    @Test
    void rejectsMoreThanTwoToolCalls() {
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.completeWithTools(anyString(), anyList(), anyList()))
                .thenReturn(ChatModelResponse.toolCalls(List.of(
                        new ChatToolCall("a", new ObjectMapper().createObjectNode()),
                        new ChatToolCall("b", new ObjectMapper().createObjectNode()),
                        new ChatToolCall("c", new ObjectMapper().createObjectNode()))));
        AiCapabilityRegistry registry = new AiCapabilityRegistry(List.of(
                capability("a"), capability("b"), capability("c")));
        AiToolOrchestrator orchestrator = new AiToolOrchestrator(registry, model, new ObjectMapper());

        assertThatThrownBy(() -> orchestrator.decide(ACTOR, "查数据", List.of(), ignored -> { }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最多执行 2 个");
    }

    private static AiCapability capability(String id) {
        AiCapability capability = mock(AiCapability.class);
        when(capability.descriptor()).thenReturn(new CapabilityDescriptor(
                id, id, "test", "READ", null, true, Set.of(RoleCode.MEMBER),
                new ObjectMapper().createObjectNode()));
        when(capability.supports(any())).thenReturn(true);
        return capability;
    }
}
