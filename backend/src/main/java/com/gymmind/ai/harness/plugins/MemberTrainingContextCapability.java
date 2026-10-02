package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.model.WorkoutPlan;
import com.gymmind.workout.domain.model.WorkoutRecord;
import com.gymmind.workout.domain.repository.WorkoutPlanRepository;
import com.gymmind.workout.domain.repository.WorkoutRecordRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class MemberTrainingContextCapability implements AiCapability {
    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            "member_training_context", "会员训练情况", "查询授权会员的训练记录、计划和趋势", "READ",
            "training:read", true, Set.of(RoleCode.MEMBER, RoleCode.COACH, RoleCode.GYM_ADMIN), schema());

    private final WorkoutRecordRepository records;
    private final WorkoutPlanRepository plans;
    private final MemberRepository members;
    private final CoachAssignmentService assignments;

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("memberId").put("type", "integer");
        return schema;
    }

    public MemberTrainingContextCapability(WorkoutRecordRepository records, WorkoutPlanRepository plans,
                                           MemberRepository members, CoachAssignmentService assignments) {
        this.records = records;
        this.plans = plans;
        this.members = members;
        this.assignments = assignments;
    }

    @Override public CapabilityDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        Long tenantId = ActorAccess.tenantId(actor);
        Long memberId = resolveMemberId(actor, arguments);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(89);
        List<WorkoutRecord> recent = records.findRecentCompleted(tenantId, memberId, from, to, 20);
        List<WorkoutPlan> memberPlans = plans.findAllByTenantId(tenantId).stream()
                .filter(plan -> memberId.equals(plan.getMemberId())).limit(10).toList();
        StringBuilder text = new StringBuilder("【会员训练情况】近90天训练记录：")
                .append(recent.size()).append("次；当前计划：").append(memberPlans.size()).append("个");
        if (!recent.isEmpty()) {
            text.append("；最近训练日期：").append(recent.get(0).getWorkoutDate())
                    .append("，时长：").append(recent.get(0).getDuration()).append("分钟");
        }
        return new ToolResult(true, text.toString(),
                List.of(new ContextSegment("member-training:" + tenantId + ":" + memberId,
                        "TOOL_RESULT", text.toString())));
    }

    private Long resolveMemberId(CurrentActor actor, JsonNode arguments) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (actor.roles().contains(RoleCode.MEMBER)) {
            Member member = members.findByTenantIdAndUserId(tenantId, actor.userId())
                    .orElseThrow(() -> new IllegalArgumentException("当前会员没有可用训练数据"));
            return member.getId();
        }
        Long requested = arguments == null ? null : arguments.path("memberId").isNumber()
                ? arguments.path("memberId").asLong() : null;
        if (requested == null || requested <= 0) throw new IllegalArgumentException("需要提供会员范围");
        if (actor.roles().contains(RoleCode.COACH)
                && (assignments == null || !assignments.canAccess(actor, requested))) {
            throw new IllegalArgumentException("当前身份无权查看该会员");
        }
        if (members.findByTenantIdAndId(tenantId, requested).isEmpty()) {
            throw new IllegalArgumentException("当前身份无权查看该会员");
        }
        return requested;
    }
}
