package com.gymmind.ai.domain;

import com.gymmind.ai.harness.application.AiChatTurn;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 回答路径规则测试：新增联网工具路由与原有规则回归 */
class AnswerPathRuleEngineTest {

    private final AnswerPathRuleEngine engine = new AnswerPathRuleEngine();

    private static final List<AiChatTurn> NO_HISTORY = List.of();

    @Test
    void 包含URL时路由到联网工具() {
        AnswerPathPlan plan = engine.match(RoleCode.MEMBER.name(),
                "https://example.com/article 这个页面讲了什么？", NO_HISTORY).orElseThrow();
        assertThat(plan.path()).isEqualTo(AnswerPath.TOOLS_ONLY);
        assertThat(plan.runToolOrchestration()).isTrue();
        assertThat(plan.preferredToolIds()).isEmpty();
    }

    @Test
    void 联网意图关键词路由到联网工具() {
        AnswerPathPlan plan = engine.match(RoleCode.MEMBER.name(),
                "帮我搜一下最新的健身趋势", NO_HISTORY).orElseThrow();
        assertThat(plan.path()).isEqualTo(AnswerPath.TOOLS_ONLY);
        assertThat(plan.routeReason()).contains("联网");
    }

    @Test
    void 运营数据问题保持原有路由() {
        AnswerPathPlan plan = engine.match(RoleCode.GYM_ADMIN.name(),
                "上个月来了多少会员？", NO_HISTORY).orElseThrow();
        assertThat(plan.path()).isEqualTo(AnswerPath.TOOLS_ONLY);
        assertThat(plan.preferredToolIds()).containsExactly("gym_operations_metrics");
    }

    @Test
    void 寒暄直接回答不受影响() {
        AnswerPathPlan plan = engine.match(RoleCode.MEMBER.name(), "你好", NO_HISTORY).orElseThrow();
        assertThat(plan.path()).isEqualTo(AnswerPath.DIRECT_LLM);
    }

    @Test
    void 知识库问题不受影响() {
        AnswerPathPlan plan = engine.match(RoleCode.MEMBER.name(), "深蹲动作有哪些常见错误？",
                NO_HISTORY).orElseThrow();
        assertThat(plan.path()).isEqualTo(AnswerPath.KNOWLEDGE_ONLY);
    }

    @Test
    void 普通问题不命中规则() {
        assertThat(engine.match(RoleCode.MEMBER.name(), "今天天气怎么样", NO_HISTORY)).isEmpty();
    }
}
