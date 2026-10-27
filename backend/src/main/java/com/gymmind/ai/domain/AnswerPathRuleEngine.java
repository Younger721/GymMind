package com.gymmind.ai.domain;

import com.gymmind.ai.harness.application.AiChatTurn;
import com.gymmind.iam.domain.model.RoleCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** 基于关键词与上下文的确定性回答路径规则（L1）。 */
@Component
public class AnswerPathRuleEngine {

    public Optional<AnswerPathPlan> match(String roleCode, String question, List<AiChatTurn> history) {
        if (question == null || question.isBlank()) {
            return Optional.empty();
        }
        String q = normalize(question);

        if (containsAny(q, "你好", "您好", "在吗", "谢谢", "感谢", "再见")) {
            return Optional.of(plan(AnswerPath.DIRECT_LLM, false, false, Set.of(),
                    "识别为寒暄，直接回答", RouteConfidence.RULE));
        }

        if (RoleCode.PLATFORM_ADMIN.name().equals(roleCode)) {
            if (containsAny(q, "ai使用", "使用情况", "调用量", "ai调用", "配额", "租户ai", "租户状态", "检查租户",
                    "平台ai", "启用了ai", "已启用ai", "活跃租户", "多少租户", "智能体", "token")) {
                return Optional.of(plan(AnswerPath.TOOLS_ONLY, false, true, Set.of("platform_ai_admin_metrics"),
                        "识别为平台 AI 管理问题，查询平台统计数据", RouteConfidence.RULE));
            }
            return Optional.empty();
        }

        if (containsAny(q, "流失", "风险名单", "预警", "久未到店", "流失风险")) {
            return Optional.of(plan(AnswerPath.TOOLS_ONLY, false, true, Set.of("member_churn_risk_list"),
                    "识别为会员流失预警查询", RouteConfidence.RULE));
        }

        if (containsAny(q, "上个月", "上月", "上上个月", "环比", "对比", "变化多少", "多少会员", "会员数",
                "到店", "签到", "预约率", "到店率", "会籍", "到期", "快到期", "活跃会员", "未完课", "运营数据", "运营指标")) {
            if (followUpMetricsQuestion(q, history)) {
                return Optional.of(plan(AnswerPath.TOOLS_ONLY, false, true, Set.of("gym_operations_metrics"),
                        "识别为运营数据追问", RouteConfidence.RULE));
            }
            if (!containsAny(q, "规范", "制度", "流程", "怎么办", "依据")) {
                return Optional.of(plan(AnswerPath.TOOLS_ONLY, false, true, Set.of("gym_operations_metrics"),
                        "识别为门店运营数据查询", RouteConfidence.RULE));
            }
        }

        if (containsAny(q, "规范", "制度", "流程", "怎么办", "动作要点", "动作", "深蹲", "卧推", "硬拉",
                "安全", "退课", "依据", "手册", "合规", "饮食", "减脂", "增肌", "训练几次", "怎么吃")) {
            return Optional.of(plan(AnswerPath.KNOWLEDGE_ONLY, true, false, Set.of(),
                    "识别为制度或知识库问题", RouteConfidence.RULE));
        }

        return Optional.empty();
    }

    private static boolean followUpMetricsQuestion(String q, List<AiChatTurn> history) {
        if (!containsAny(q, "变化", "对比", "相比", "增减", "多少")) {
            return false;
        }
        if (history == null || history.isEmpty()) {
            return false;
        }
        for (int i = history.size() - 1; i >= 0 && i >= history.size() - 3; i--) {
            AiChatTurn turn = history.get(i);
            if (turn != null && "assistant".equalsIgnoreCase(turn.role()) && turn.content() != null
                    && turn.content().matches(".*\\d+.*")) {
                return true;
            }
        }
        return false;
    }

    private static AnswerPathPlan plan(AnswerPath path, boolean rag, boolean tools, Set<String> preferred,
                                       String reason, RouteConfidence confidence) {
        return new AnswerPathPlan(path, rag, tools, preferred, reason, confidence);
    }

    private static String normalize(String question) {
        return question.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT).replaceAll("\\s+", ""))) {
                return true;
            }
        }
        return false;
    }
}
