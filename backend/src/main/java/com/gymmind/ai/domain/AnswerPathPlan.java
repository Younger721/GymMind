package com.gymmind.ai.domain;

import java.util.Set;

/** 单次问答的执行计划，由路由器产出、Harness 消费。 */
public record AnswerPathPlan(
        AnswerPath path,
        boolean runKnowledgeSearch,
        boolean runToolOrchestration,
        Set<String> preferredToolIds,
        String routeReason,
        RouteConfidence confidence) {

    public AnswerPathPlan {
        preferredToolIds = preferredToolIds == null ? Set.of() : Set.copyOf(preferredToolIds);
    }

    public AnswerPathPlan withExecutionFlags(boolean knowledge, boolean tools) {
        return new AnswerPathPlan(path, knowledge, tools, preferredToolIds, routeReason, confidence);
    }
}
