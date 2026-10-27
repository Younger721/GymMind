package com.gymmind.ai.chat.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AiRoleProfileResolver {

    private static final List<String> PLATFORM_SAMPLES = List.of(
            "全平台本月 AI 调用了多少次？",
            "有多少租户启用了 AI？",
            "现在有多少活跃租户？",
            "平台一共有多少租户？");

    private static final List<String> PLATFORM_STORE_SAMPLES = List.of(
            "当前门店本月 AI 配额和用量是多少？",
            "当前门店是否已启用 AI？",
            "当前门店智能体和知识库配额用了多少？");

    private static final List<String> GYM_OPS_SAMPLES = List.of(
            "上个月来了多少会员？",
            "和上上个月相比变化多少？",
            "哪些会员流失风险高？",
            "本月有哪些会员会籍快到期？",
            "最近预约到店率怎么样？");

    private static final List<String> GYM_OPS_PLUGINS = List.of(
            "basic_chat", "knowledge_search", "gym_operations_metrics", "member_churn_risk_list",
            "course_booking_context", "member_training_context");

    public AiRoleProfile resolve(CurrentActor actor) {
        RoleCode role = actor.isPlatformAdmin() ? RoleCode.PLATFORM_ADMIN
                : actor.roles().contains(RoleCode.GYM_ADMIN) ? RoleCode.GYM_ADMIN
                : actor.roles().contains(RoleCode.COACH) ? RoleCode.COACH : RoleCode.MEMBER;
        return switch (role) {
            case PLATFORM_ADMIN -> resolvePlatformAdmin(actor);
            case GYM_ADMIN -> new AiRoleProfile("GYM_ADMIN", "门店运营助手",
                    "你是健身房门店运营助手，只回答当前门店的会员、课程、预约、运营和 AI 配置问题。不得访问其他门店。门店汇总数据用 gym_operations_metrics；逐会员流失风险名单用 member_churn_risk_list。",
                    GYM_OPS_SAMPLES, GYM_OPS_PLUGINS, 10);
            case COACH -> new AiRoleProfile("COACH", "教练工作助手",
                    "你是教练工作助手，可协助当前门店授权范围内的会员训练指导、动作建议和训练计划草案。不得访问未授权会员。",
                    List.of("我负责的会员最近训练得怎么样？", "本周会员约了哪些训练课？", "帮我看会员的训练计划排期", "这个动作有哪些要点和常见错误？", "给会员写一份本周训练建议"),
                    List.of("basic_chat", "knowledge_search", "member_training_context", "training_plan_calendar", "course_booking_context"), 10);
            case MEMBER -> new AiRoleProfile("MEMBER", "我的训练助手",
                    "你是会员私人健身助手，只能使用当前会员自己的训练、课程和偏好数据，提供安全、清晰的健身建议。不要查看或推测其他人的数据。",
                    List.of("我的训练计划是什么？", "我本周还有哪些课？", "深蹲动作有哪些常见错误？", "新手每周训练几次比较合适？", "减脂期饮食要注意什么？"),
                    List.of("basic_chat", "knowledge_search", "member_training_context", "training_plan_calendar", "course_booking_context"), 10);
        };
    }

    /** 平台管理员始终走平台 AI 问答，顶栏选中门店只补充该店配额类问题。 */
    private static AiRoleProfile resolvePlatformAdmin(CurrentActor actor) {
        List<String> samples = new ArrayList<>(PLATFORM_SAMPLES);
        String storeHint = "";
        if (ActorAccess.tenantId(actor) != null) {
            samples.addAll(PLATFORM_STORE_SAMPLES);
            storeHint = "顶栏已选门店时，该门店的 AI 启用状态、配额和本月用量仍用 platform_ai_admin_metrics 查询，不要改问会员、课程或到店数据。";
        }
        return new AiRoleProfile("PLATFORM_ADMIN", "平台 AI 管理助手",
                "你是平台 AI 管理助手，只回答平台租户、AI 配额、调用统计和平台配置问题。"
                        + "全平台调用量、启用 AI 租户数、活跃租户、租户配额与本月用量须通过 platform_ai_admin_metrics 工具查询，不要向用户索要 tenantId 来替代工具。"
                        + storeHint
                        + "不要回答单店会员、课程、预约、流失和训练动作问题。",
                List.copyOf(samples),
                List.of("basic_chat", "knowledge_search", "platform_ai_admin_metrics"),
                10);
    }
}
