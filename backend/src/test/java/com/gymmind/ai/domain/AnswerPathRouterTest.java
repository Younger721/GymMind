package com.gymmind.ai.domain;

import com.gymmind.ai.chat.application.AiRoleProfile;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerPathRouterTest {

    private final AnswerPathRouter router = new AnswerPathRouter(new AnswerPathRuleEngine(), null);
    private static final CurrentActor GYM_ADMIN = new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN),
            Set.of("ai:chat", "analytics:read"), 0, "t");
    private static final AiRoleProfile ADMIN_PROFILE = new AiRoleProfile(
            "GYM_ADMIN", "门店运营助手", "prompt", List.of(), List.of("basic_chat", "knowledge_search",
            "gym_operations_metrics", "member_churn_risk_list"), 10);

    @Test
    void routesStoreMetricsWithoutKnowledgeSearch() {
        AiHarnessProperties props = new AiHarnessProperties();
        AnswerPathPlan plan = router.resolve(GYM_ADMIN, ADMIN_PROFILE, "上个月来了多少会员？",
                List.of(), ADMIN_PROFILE.enabledPlugins(), props);

        assertThat(plan.path()).isEqualTo(AnswerPath.TOOLS_ONLY);
        assertThat(plan.runKnowledgeSearch()).isFalse();
        assertThat(plan.runToolOrchestration()).isTrue();
        assertThat(plan.preferredToolIds()).containsExactly("gym_operations_metrics");
    }

    @Test
    void routesChurnListToDedicatedTool() {
        AnswerPathPlan plan = router.resolve(GYM_ADMIN, ADMIN_PROFILE, "哪些会员流失风险高？",
                List.of(), ADMIN_PROFILE.enabledPlugins(), new AiHarnessProperties());

        assertThat(plan.path()).isEqualTo(AnswerPath.TOOLS_ONLY);
        assertThat(plan.preferredToolIds()).containsExactly("member_churn_risk_list");
    }

    @Test
    void legacyModeAlwaysRetrievesKnowledge() {
        AiHarnessProperties props = new AiHarnessProperties();
        props.setRoutingMode(AiHarnessProperties.RoutingMode.LEGACY_ALWAYS_RAG);
        AnswerPathPlan plan = router.resolve(GYM_ADMIN, ADMIN_PROFILE, "上个月来了多少会员？",
                List.of(), ADMIN_PROFILE.enabledPlugins(), props);

        assertThat(plan.runKnowledgeSearch()).isTrue();
        assertThat(plan.confidence()).isEqualTo(RouteConfidence.LEGACY);
    }

    @Test
    void chitchatSkipsRagAndTools() {
        AnswerPathPlan plan = router.resolve(GYM_ADMIN, ADMIN_PROFILE, "你好",
                List.of(), ADMIN_PROFILE.enabledPlugins(), new AiHarnessProperties());

        assertThat(plan.path()).isEqualTo(AnswerPath.DIRECT_LLM);
        assertThat(plan.runKnowledgeSearch()).isFalse();
        assertThat(plan.runToolOrchestration()).isFalse();
        assertThat(plan.routeReason()).contains("寒暄");
    }

    @Test
    void platformAdminUsageRoutesToPlatformTool() {
        AiRoleProfile platformProfile = new AiRoleProfile(
                "PLATFORM_ADMIN", "平台 AI 管理助手", "prompt",
                List.of("查看 AI 使用情况"), List.of("basic_chat", "knowledge_search", "platform_ai_admin_metrics"), 10);
        CurrentActor platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:stats:read", "ai:chat"), 0, "t");
        List<String> enabled = List.of("basic_chat", "knowledge_search", "platform_ai_admin_metrics");

        AnswerPathPlan plan = router.resolve(platformAdmin, platformProfile, "查看 AI 使用情况",
                List.of(), enabled, new AiHarnessProperties());

        assertThat(plan.runToolOrchestration()).isTrue();
        assertThat(plan.runKnowledgeSearch()).isFalse();
        assertThat(plan.preferredToolIds()).containsExactly("platform_ai_admin_metrics");

        AnswerPathPlan gymQuestion = router.resolve(platformAdmin, platformProfile, "上个月来了多少会员？",
                List.of(), enabled, new AiHarnessProperties());
        assertThat(gymQuestion.preferredToolIds()).doesNotContain("gym_operations_metrics");
    }
}
