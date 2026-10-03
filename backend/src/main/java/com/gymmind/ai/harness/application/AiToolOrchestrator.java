package com.gymmind.ai.harness.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.application.ChatModelResponse;
import com.gymmind.ai.application.ChatToolCall;
import com.gymmind.ai.application.ChatToolDefinition;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.AiCapabilityRegistry;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.springframework.stereotype.Component
public class AiToolOrchestrator {
    public static final int MAX_TOOL_CALLS = 2;
    private static final Logger log = LoggerFactory.getLogger(AiToolOrchestrator.class);
    private final AiCapabilityRegistry registry;
    private final ChatModelGateway model;
    private final ObjectMapper mapper;

    public AiToolOrchestrator(AiCapabilityRegistry registry, ChatModelGateway model, ObjectMapper mapper) {
        this.registry = registry;
        this.model = model;
        this.mapper = mapper;
    }

    public AiToolOrchestrationResult decide(CurrentActor actor, String question,
                                             List<ContextSegment> context,
                                             Consumer<AiProgressEvent> progress) {
        return decide(actor, question, context, List.of(), progress);
    }

    public AiToolOrchestrationResult decide(CurrentActor actor, String question,
                                             List<ContextSegment> context,
                                             Collection<String> enabledIds,
                                             Consumer<AiProgressEvent> progress) {
        List<AiCapability> capabilities = registry.listFor(actor, enabledIds);
        capabilities = capabilities.stream()
                .filter(capability -> !"basic_chat".equals(capability.descriptor().id()))
                .filter(capability -> !"knowledge_search".equals(capability.descriptor().id()))
                .filter(capability -> "READ".equalsIgnoreCase(capability.descriptor().type()))
                .toList();
        List<ChatToolDefinition> definitions = capabilities.stream()
                .map(c -> new ChatToolDefinition(c.descriptor().id(), c.descriptor().description(), c.descriptor().parameterSchema()))
                .toList();
        java.util.Set<String> candidateIds = capabilities.stream()
                .map(capability -> capability.descriptor().id()).collect(java.util.stream.Collectors.toSet());
        long started = System.currentTimeMillis();
        ChatModelResponse response = model.completeWithTools(question, context, definitions);
        if (response == null) return new AiToolOrchestrationResult(null, List.of(), List.of());
        if (!response.hasToolCalls()) {
            progress.accept(new AiProgressEvent("orchestrator", "tool_decision", AiProgressEvent.Status.COMPLETED,
                    "直接回答", 0));
            return new AiToolOrchestrationResult(response.content(), List.of(), List.of(), response.usage(),
                    Math.max(0, System.currentTimeMillis() - started));
        }
        if (response.toolCalls().size() > MAX_TOOL_CALLS) {
            throw new IllegalArgumentException("一次最多执行 2 个只读工具");
        }
        String traceId = UUID.randomUUID().toString();
        progress.accept(new AiProgressEvent("orchestrator", "tool_decision", AiProgressEvent.Status.COMPLETED,
                "已识别需要查询业务数据", response.toolCalls().size()));
        List<ToolResult> results = new ArrayList<>();
        List<ContextSegment> toolContext = new ArrayList<>();
        for (ChatToolCall call : response.toolCalls()) {
            if (!candidateIds.contains(call.name())) {
                throw new IllegalArgumentException("工具不支持：" + call.name());
            }
            AiCapability capability = registry.find(call.name())
                    .orElseThrow(() -> new IllegalArgumentException("工具不支持：" + call.name()));
            if (!capability.supports(actor)) {
                throw new IllegalArgumentException("当前身份无权使用该工具");
            }
            if (!"READ".equalsIgnoreCase(capability.descriptor().type())) {
                throw new IllegalArgumentException("首期只允许只读工具");
            }
            validateArguments(call.arguments(), capability.descriptor().parameterSchema());
            progress.accept(new AiProgressEvent(call.name(), "tool_start", AiProgressEvent.Status.STARTED,
                    "正在查询 " + capability.descriptor().name(), null));
            ToolResult result = capability.execute(actor, call.arguments(),
                    new ToolExecutionContext(traceId, question, progress));
            results.add(result);
            toolContext.addAll(result.context());
            log.info("AI tool traceId={} actor={} tenant={} tool={} params={} status={}",
                    traceId, actor.userId(), ActorAccess.tenantId(actor), call.name(), summarizeArguments(call.arguments()),
                    result.success() ? "SUCCESS" : "FAILED");
            progress.accept(new AiProgressEvent(call.name(), "tool_result",
                    result.success() ? AiProgressEvent.Status.COMPLETED : AiProgressEvent.Status.FAILED,
                    result.success() ? "查询完成" : "数据暂时不可用", result.context().size()));
        }
        return new AiToolOrchestrationResult(null, results, toolContext, response.usage(),
                Math.max(0, System.currentTimeMillis() - started));
    }

    /** Deterministic read-only fallback used when the model cannot call tools. */
    public AiToolOrchestrationResult executeNamed(CurrentActor actor, String question,
                                                    List<String> ids, Consumer<AiProgressEvent> progress) {
        List<ToolResult> results = new ArrayList<>();
        List<ContextSegment> context = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return new AiToolOrchestrationResult(null, results, context);
        if (ids.size() > MAX_TOOL_CALLS) throw new IllegalArgumentException("一次最多执行 2 个只读工具");
        ToolExecutionContext executionContext = new ToolExecutionContext(UUID.randomUUID().toString(), question, progress);
        for (String id : ids) {
            AiCapability capability = registry.find(id).orElseThrow(() -> new IllegalArgumentException("工具不支持：" + id));
            if (!capability.supports(actor)) throw new IllegalArgumentException("当前身份无权使用该工具");
            if (!"READ".equalsIgnoreCase(capability.descriptor().type())) {
                throw new IllegalArgumentException("首期只允许只读工具");
            }
            progress.accept(new AiProgressEvent(id, "tool_start", AiProgressEvent.Status.STARTED,
                    "正在查询 " + capability.descriptor().name(), null));
            ToolResult result = capability.execute(actor,
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(), executionContext);
            results.add(result);
            context.addAll(result.context());
            progress.accept(new AiProgressEvent(id, "tool_result",
                    result.success() ? AiProgressEvent.Status.COMPLETED : AiProgressEvent.Status.FAILED,
                    result.success() ? "查询完成" : "数据暂时不可用", result.context().size()));
        }
        return new AiToolOrchestrationResult(null, results, context);
    }

    private static String summarizeArguments(JsonNode arguments) {
        if (arguments == null || !arguments.isObject()) return "{}";
        List<String> names = new ArrayList<>();
        arguments.fieldNames().forEachRemaining(names::add);
        return names.toString();
    }

    private void validateArguments(JsonNode arguments, JsonNode schema) {
        if (arguments == null || !arguments.isObject()) throw new IllegalArgumentException("工具参数必须是 JSON 对象");
        if (arguments.has("sql") || arguments.has("querySql") || arguments.has("rawQuery")) {
            throw new IllegalArgumentException("工具不接受 SQL 或任意查询语句");
        }
        JsonNode required = schema == null ? null : schema.path("required");
        if (required != null && required.isArray()) {
            for (JsonNode field : required) {
                if (field.isTextual() && !arguments.has(field.asText())) {
                    throw new IllegalArgumentException("缺少工具参数：" + field.asText());
                }
            }
        }
        JsonNode properties = schema == null ? null : schema.path("properties");
        if (properties != null && properties.isObject()) {
            properties.fields().forEachRemaining(entry -> {
                JsonNode value = arguments.get(entry.getKey());
                if (value == null) return;
                String type = entry.getValue().path("type").asText("");
                if ("integer".equals(type) && !value.isIntegralNumber()) {
                    throw new IllegalArgumentException("工具参数类型错误：" + entry.getKey());
                }
                if ("string".equals(type) && !value.isTextual()) {
                    throw new IllegalArgumentException("工具参数类型错误：" + entry.getKey());
                }
            });
        }
    }
}
