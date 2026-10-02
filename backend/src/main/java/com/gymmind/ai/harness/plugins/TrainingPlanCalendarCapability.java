package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.workout.domain.model.MemberWorkoutSchedule;
import com.gymmind.workout.infrastructure.persistence.SpringDataMemberWorkoutScheduleRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class TrainingPlanCalendarCapability implements AiCapability {
    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            "training_plan_calendar", "训练计划日历", "查询会员训练计划和日期排期", "READ",
            "training:read", true, Set.of(RoleCode.MEMBER, RoleCode.COACH, RoleCode.GYM_ADMIN), schema());

    private final SpringDataMemberWorkoutScheduleRepository schedules;
    private final MemberRepository members;
    private final CoachAssignmentService assignments;

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("memberId").put("type", "integer");
        properties.putObject("from").put("type", "string").put("format", "date");
        properties.putObject("to").put("type", "string").put("format", "date");
        return schema;
    }

    public TrainingPlanCalendarCapability(SpringDataMemberWorkoutScheduleRepository schedules, MemberRepository members,
                                          CoachAssignmentService assignments) {
        this.schedules = schedules;
        this.members = members;
        this.assignments = assignments;
    }

    @Override public CapabilityDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        Long tenantId = ActorAccess.tenantId(actor);
        Long memberId = actor.roles().contains(RoleCode.MEMBER)
                ? members.findByTenantIdAndUserId(tenantId, actor.userId()).map(m -> m.getId()).orElse(null)
                : arguments != null && arguments.path("memberId").isNumber()
                ? arguments.path("memberId").asLong() : null;
        if (memberId == null || memberId <= 0) throw new IllegalArgumentException("当前身份没有可用训练日历");
        if (actor.roles().contains(RoleCode.COACH)
                && (assignments == null || !assignments.canAccess(actor, memberId))) {
            throw new IllegalArgumentException("当前身份无权查看该会员日历");
        }
        if (members.findByTenantIdAndId(tenantId, memberId).isEmpty()) {
            throw new IllegalArgumentException("当前身份无权查看该会员日历");
        }
        LocalDate from = date(arguments, "from", LocalDate.now().withDayOfMonth(1));
        LocalDate to = date(arguments, "to", from.plusMonths(1).minusDays(1));
        if (to.isBefore(from) || to.isAfter(from.plusDays(366))) throw new IllegalArgumentException("日期范围无效");
        List<MemberWorkoutSchedule> entries = schedules.findAllByTenantIdAndMemberIdAndScheduledDateBetween(
                tenantId, memberId, from, to);
        String text = "【训练计划日历】" + from + "至" + to + "共安排" + entries.size() + "个训练计划";
        return ToolResult.context("training-calendar:" + tenantId + ":" + memberId, text);
    }

    private static LocalDate date(JsonNode args, String name, LocalDate fallback) {
        String value = args == null ? null : args.path(name).asText(null);
        return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
    }
}
