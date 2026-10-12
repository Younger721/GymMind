package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.analytics.application.MemberChurnRiskService;
import com.gymmind.analytics.application.MemberChurnRiskView;
import com.gymmind.analytics.domain.ChurnRiskLevel;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 店长专用：按规则引擎查询逐会员流失风险名单（只读查库，非 RAG）。 */
@Component
public class MemberChurnRiskCapability implements AiCapability {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final DateTimeFormatter CHECK_IN_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.CHINA)
            .withZone(ZoneId.systemDefault());

    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            "member_churn_risk_list",
            "会员流失预警名单",
            "查询当前门店会员活跃度与流失风险（签到、预约、完课、会籍），返回可解释的风险分与原因；"
                    + "门店汇总指标请使用 gym_operations_metrics",
            "READ",
            "analytics:read",
            true,
            Set.of(RoleCode.GYM_ADMIN),
            schema());

    private final MemberChurnRiskService churnRisks;

    public MemberChurnRiskCapability(MemberChurnRiskService churnRisks) {
        this.churnRisks = churnRisks;
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean supports(CurrentActor actor) {
        return actor != null
                && actor.roles().contains(RoleCode.GYM_ADMIN)
                && ActorAccess.tenantId(actor) != null
                && actor.hasPermission("analytics:read");
    }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        if (!supports(actor)) {
            return ToolResult.error("当前身份无权查看会员流失预警");
        }
        ChurnRiskLevel minLevel = parseMinLevel(arguments);
        int limit = parseLimit(arguments);
        List<MemberChurnRiskView> rows = churnRisks.list(actor).stream()
                .filter(row -> atLeast(row.level(), minLevel))
                .limit(limit)
                .toList();
        String text = formatList(rows, minLevel, limit);
        Long tenantId = ActorAccess.tenantId(actor);
        return ToolResult.context("member-churn-risk:" + tenantId, text);
    }

    private static ChurnRiskLevel parseMinLevel(JsonNode arguments) {
        if (arguments == null || arguments.path("minLevel").isMissingNode()) {
            return ChurnRiskLevel.LOW;
        }
        String raw = arguments.path("minLevel").asText("LOW").trim().toUpperCase(Locale.ROOT);
        try {
            return ChurnRiskLevel.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("minLevel 仅支持 LOW、MEDIUM、HIGH");
        }
    }

    private static int parseLimit(JsonNode arguments) {
        if (arguments == null || !arguments.path("limit").isNumber()) {
            return DEFAULT_LIMIT;
        }
        int limit = arguments.path("limit").asInt(DEFAULT_LIMIT);
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit 范围为 1～" + MAX_LIMIT);
        }
        return limit;
    }

    private static boolean atLeast(ChurnRiskLevel level, ChurnRiskLevel minLevel) {
        return level.ordinal() >= minLevel.ordinal();
    }

    static String formatList(List<MemberChurnRiskView> rows, ChurnRiskLevel minLevel, int limit) {
        StringBuilder out = new StringBuilder("【会员流失预警名单】筛选等级≥")
                .append(minLevel.name())
                .append("，最多 ")
                .append(limit)
                .append(" 人；共 ")
                .append(rows.size())
                .append(" 人\n");
        if (rows.isEmpty()) {
            out.append("当前筛选条件下暂无需要跟进的会员。");
            return out.toString();
        }
        int index = 1;
        for (MemberChurnRiskView row : rows) {
            out.append(index++).append(". ")
                    .append(row.memberName() == null ? "会员" : row.memberName())
                    .append("（ID=").append(row.memberId()).append("）")
                    .append(" 风险=").append(levelLabel(row.level()))
                    .append(" 分数=").append(row.score())
                    .append("；最近到店=").append(formatCheckIn(row.lastCheckInAt()))
                    .append("；30天到店=").append(row.checkInsLast30Days()).append("次")
                    .append("；30天预约=").append(row.bookingsLast30Days()).append("次")
                    .append("；课程参与=").append(row.courseParticipationsLast30Days()).append("次")
                    .append("；会籍剩余=").append(row.membershipDaysRemaining()).append("天")
                    .append("；原因=").append(row.reasons() == null || row.reasons().isEmpty()
                            ? "暂无明显规则命中" : String.join("、", row.reasons()))
                    .append("\n");
        }
        return out.toString().trim();
    }

    private static String levelLabel(ChurnRiskLevel level) {
        return switch (level) {
            case HIGH -> "高";
            case MEDIUM -> "中";
            case LOW -> "低";
        };
    }

    private static String formatCheckIn(Instant instant) {
        return instant == null ? "暂无记录" : CHECK_IN_TIME.format(instant);
    }

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode minLevel = properties.putObject("minLevel");
        minLevel.put("type", "string");
        ArrayNode levelEnum = minLevel.putArray("enum");
        levelEnum.add("LOW").add("MEDIUM").add("HIGH");
        properties.putObject("limit").put("type", "integer");
        return schema;
    }
}
