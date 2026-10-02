package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.util.Set;

/** 将门店运营插件以结构化 JSON 工具形式暴露给模型。 */
@Component
public class GymOperationsMetricsCapability implements AiCapability {
    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            "gym_operations_metrics", "门店运营数据",
            "按白名单指标、时间范围和时间维度查询当前健身房运营汇总，不接受 SQL",
            "READ", "analytics:read", true, Set.of(com.gymmind.iam.domain.model.RoleCode.GYM_ADMIN), schema());

    private final GymOperationsMetricsTool tool;

    @Autowired
    public GymOperationsMetricsCapability(GymOperationsMetricsTool tool) {
        this.tool = tool;
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean supports(CurrentActor actor) {
        return tool != null && tool.supports(actor);
    }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        if (tool == null) throw new IllegalStateException("运营工具未初始化");
        return tool.executeStructured(actor, arguments, context);
    }

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode metrics = properties.putObject("metrics");
        metrics.put("type", "array");
        ArrayNode metricItems = metrics.putObject("items").putArray("enum");
        for (OperationsMetric metric : OperationsMetric.values()) metricItems.add(metric.name());
        ObjectNode period = properties.putObject("period");
        period.put("type", "string");
        ArrayNode periodEnum = period.putArray("enum");
        periodEnum.add("PREVIOUS_MONTH").add("PREVIOUS_PREVIOUS_MONTH")
                .add("CURRENT_MONTH").add("LAST_7_DAYS").add("LAST_30_DAYS");
        properties.putObject("from").put("type", "string").put("format", "date");
        properties.putObject("to").put("type", "string").put("format", "date");
        ObjectNode groupBy = properties.putObject("groupBy");
        groupBy.put("type", "string");
        ArrayNode groupEnum = groupBy.putArray("enum");
        for (OperationsGroupBy group : OperationsGroupBy.values()) groupEnum.add(group.name());
        properties.putObject("comparePrevious").put("type", "boolean");
        return schema;
    }
}
