package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 门店运营数据工具：固定白名单 SQL，模型永远不能传入任意 SQL。 */
@Component
public class GymOperationsMetricsTool implements AiHarnessPlugin {
    private static final AiHarnessPluginDescriptor DESCRIPTOR = new AiHarnessPluginDescriptor(
            "gym_operations_metrics", "门店运营数据", "查询当前健身房签到、预约、会籍和训练活跃数据", "TOOL", "analytics:read", true, true);
    private static final String QUESTION_KEYWORDS = "到店|签到|来了|活跃|预约|到店率|会籍|续费|到期|训练次数|训练活跃|会员减少|会员增加|经营|运营|趋势|变化|对比|比较|环比";
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public GymOperationsMetricsTool(JdbcTemplate jdbc) {
        this(jdbc, Clock.systemDefaultZone());
    }

    GymOperationsMetricsTool(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override public AiHarnessPluginDescriptor descriptor() { return DESCRIPTOR; }

    @Override public boolean supports(CurrentActor actor) {
        return actor != null && actor.roles().contains(RoleCode.GYM_ADMIN)
                && ActorAccess.tenantId(actor) != null && actor.hasPermission("analytics:read");
    }

    @Override public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
        if (!matchesQuestion(input)) return;
        Long tenantId = ActorAccess.tenantId(actor);
        if (tenantId == null) return;
        OperationsMetricsQuery query = resolveQuery(input, LocalDate.now(clock));
        OperationsMetricsContext metrics = query(tenantId, query);
        context.add(new ContextSegment("gym-operations:" + tenantId + ":" + query.from(), "gym_operations_metrics", format(metrics)));
    }

    /** 执行经过 JSON Schema 和白名单校验的结构化查询意图。 */
    ToolResult executeStructured(CurrentActor actor, JsonNode arguments, ToolExecutionContext executionContext) {
        if (!supports(actor)) return ToolResult.error("当前身份无权使用门店运营数据");
        try {
            OperationsQueryIntent intent = parseStructuredIntent(arguments, LocalDate.now(clock));
            Long tenantId = ActorAccess.tenantId(actor);
            OperationsMetricsContext metrics = query(tenantId, intent.query());
            List<String> grouped = intent.groupBy() == OperationsGroupBy.NONE
                    ? List.of()
                    : groupedCheckIns(tenantId, intent.query(), intent.groupBy(), intent.metrics());
            String text = formatStructured(metrics, intent, grouped);
            return ToolResult.context("gym-operations:" + tenantId + ":" + intent.query().from(), text);
        } catch (IllegalArgumentException exception) {
            return ToolResult.error(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolResult.error("门店运营数据暂时不可用");
        }
    }

    static OperationsQueryIntent parseStructuredIntent(JsonNode arguments, LocalDate today) {
        if (arguments == null || !arguments.isObject()) throw new IllegalArgumentException("工具参数必须是 JSON 对象");
        Set<String> allowed = Set.of("metrics", "period", "from", "to", "groupBy", "comparePrevious");
        arguments.fieldNames().forEachRemaining(name -> {
            if (name.equals("sql") || name.equals("querySql") || name.equals("rawQuery")) {
                throw new IllegalArgumentException("工具不接受 SQL 或任意查询语句");
            }
            if (!allowed.contains(name)) throw new IllegalArgumentException("不支持的查询参数：" + name);
        });
        List<OperationsMetric> metrics = new ArrayList<>();
        JsonNode metricNode = arguments.get("metrics");
        if (metricNode != null) {
            if (!metricNode.isArray() || metricNode.isEmpty() || metricNode.size() > OperationsMetric.values().length) {
                throw new IllegalArgumentException("metrics 必须是非空白名单数组");
            }
            for (JsonNode value : metricNode) {
                if (!value.isTextual()) throw new IllegalArgumentException("运营指标必须是字符串");
                try {
                    metrics.add(OperationsMetric.valueOf(value.asText()));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("不支持的运营指标：" + value.asText());
                }
            }
            metrics = new ArrayList<>(new LinkedHashSet<>(metrics));
        }
        String fromText = text(arguments, "from");
        String toText = text(arguments, "to");
        OperationsMetricsQuery query;
        if (fromText != null || toText != null) {
            if (fromText == null || toText == null) throw new IllegalArgumentException("from 和 to 必须同时提供");
            try {
                LocalDate from = LocalDate.parse(fromText);
                LocalDate to = LocalDate.parse(toText);
                query = new OperationsMetricsQuery(from, to, from + "至" + to);
            } catch (java.time.format.DateTimeParseException exception) {
                throw new IllegalArgumentException("日期必须使用 YYYY-MM-DD 格式");
            }
        } else {
            String period = text(arguments, "period");
            query = resolveStructuredPeriod(period, today);
        }
        if (query.from().plusDays(366).isBefore(query.to())) throw new IllegalArgumentException("查询时间范围不能超过 366 天");
        OperationsGroupBy groupBy = enumValue(arguments, "groupBy", OperationsGroupBy.class, OperationsGroupBy.NONE);
        JsonNode compare = arguments.get("comparePrevious");
        if (compare != null && !compare.isBoolean()) throw new IllegalArgumentException("comparePrevious 必须是布尔值");
        return new OperationsQueryIntent(metrics, query, groupBy, compare != null && compare.asBoolean());
    }

    public static boolean matchesQuestion(String question) {
        return question != null && question.matches("(?s).*?(" + QUESTION_KEYWORDS + ").*");
    }

    static OperationsMetricsQuery previousCalendarMonth(LocalDate date) {
        LocalDate first = date.withDayOfMonth(1).minusMonths(1);
        return new OperationsMetricsQuery(first, first.plusMonths(1).minusDays(1), first.getYear() + "年" + first.getMonthValue() + "月");
    }

    static BigDecimal changeRate(long current, long previous) {
        if (previous == 0) return current == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(100).setScale(1);
        return BigDecimal.valueOf(current - previous).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
    }

    private static OperationsMetricsQuery resolveStructuredPeriod(String period, LocalDate today) {
        String value = period == null || period.isBlank() ? "PREVIOUS_MONTH" : period;
        return switch (value) {
            case "PREVIOUS_MONTH" -> previousCalendarMonth(today);
            case "PREVIOUS_PREVIOUS_MONTH" -> {
                LocalDate first = today.withDayOfMonth(1).minusMonths(2);
                yield new OperationsMetricsQuery(first, first.plusMonths(1).minusDays(1), first.getYear() + "年" + first.getMonthValue() + "月");
            }
            case "CURRENT_MONTH" -> {
                LocalDate first = today.withDayOfMonth(1);
                yield new OperationsMetricsQuery(first, today, first.getYear() + "年" + first.getMonthValue() + "月至今");
            }
            case "LAST_7_DAYS" -> new OperationsMetricsQuery(today.minusDays(6), today, today.minusDays(6) + "至" + today);
            case "LAST_30_DAYS" -> new OperationsMetricsQuery(today.minusDays(29), today, today.minusDays(29) + "至" + today);
            default -> throw new IllegalArgumentException("不支持的时间范围：" + value);
        };
    }

    private OperationsMetricsQuery resolveQuery(String input, LocalDate today) {
        if (input != null && (input.contains("最近7天") || input.contains("近7天"))) {
            LocalDate first = today.minusDays(6);
            return new OperationsMetricsQuery(first, today, first + "至" + today);
        }
        if (input != null && (input.contains("最近30天") || input.contains("近30天"))) {
            LocalDate first = today.minusDays(29);
            return new OperationsMetricsQuery(first, today, first + "至" + today);
        }
        if (input != null && (input.contains("和上上个月相比")
                || input.contains("上个月和上上个月")
                || input.contains("上个月与上上个月"))) {
            return previousCalendarMonth(today);
        }
        if (input != null && input.contains("上上个月")) {
            LocalDate first = today.withDayOfMonth(1).minusMonths(2);
            return new OperationsMetricsQuery(first, first.plusMonths(1).minusDays(1), first.getYear() + "年" + first.getMonthValue() + "月");
        }
        if (input != null && input.contains("最近三个月")) {
            LocalDate first = today.withDayOfMonth(1).minusMonths(2);
            return new OperationsMetricsQuery(first, today, first.getYear() + "年" + first.getMonthValue() + "月至今");
        }
        if (input != null && (input.contains("本月") || input.contains("这个月"))) {
            LocalDate first = today.withDayOfMonth(1);
            return new OperationsMetricsQuery(first, today, first.getYear() + "年" + first.getMonthValue() + "月至今");
        }
        return previousCalendarMonth(today);
    }

    private OperationsMetricsContext query(Long tenantId, OperationsMetricsQuery q) {
        LocalDate previousFrom = q.from().minusMonths(1);
        LocalDate previousTo = q.to().minusMonths(1);
        if (q.from().getDayOfMonth() == 1
                && q.to().equals(q.from().plusMonths(1).minusDays(1))) {
            previousTo = previousFrom.withDayOfMonth(previousFrom.lengthOfMonth());
        }
        long visitors = scalar("SELECT COUNT(DISTINCT member_id) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?", tenantId, q.from(), q.to());
        long checkIns = scalar("SELECT COUNT(*) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?", tenantId, q.from(), q.to());
        long previousVisitors = scalar("SELECT COUNT(DISTINCT member_id) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?", tenantId, previousFrom, previousTo);
        long previousCheckIns = scalar("SELECT COUNT(*) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?", tenantId, previousFrom, previousTo);
        long activeMembers = scalar("SELECT COUNT(*) FROM member_profile WHERE tenant_id=? AND status='ACTIVE'", tenantId);
        long bookings = scalar("SELECT COUNT(*) FROM course_booking WHERE tenant_id=? AND DATE(created_at) BETWEEN ? AND ? AND status <> 'CANCELLED'", tenantId, q.from(), q.to());
        long completed = scalar("SELECT COUNT(*) FROM course_booking WHERE tenant_id=? AND DATE(checked_in_at) BETWEEN ? AND ? AND status='COMPLETED'", tenantId, q.from(), q.to());
        long expiring7 = scalar("SELECT COUNT(DISTINCT member_id) FROM member_membership WHERE tenant_id=? AND status='ACTIVE' AND expires_at >= CURRENT_TIMESTAMP AND expires_at < DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY)", tenantId);
        long expiring30 = scalar("SELECT COUNT(DISTINCT member_id) FROM member_membership WHERE tenant_id=? AND status='ACTIVE' AND expires_at >= CURRENT_TIMESTAMP AND expires_at < DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)", tenantId);
        long inactive = scalar("SELECT COUNT(*) FROM member_profile m WHERE m.tenant_id=? AND m.status='ACTIVE' AND NOT EXISTS (SELECT 1 FROM gym_check_in c WHERE c.tenant_id=m.tenant_id AND c.member_id=m.id AND c.check_in_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY))", tenantId);
        long workoutMembers = scalar("SELECT COUNT(DISTINCT member_id) FROM workout_record WHERE tenant_id=? AND workout_date BETWEEN ? AND ? AND status='COMPLETED'", tenantId, q.from(), q.to());
        long workoutCount = scalar("SELECT COUNT(*) FROM workout_record WHERE tenant_id=? AND workout_date BETWEEN ? AND ? AND status='COMPLETED'", tenantId, q.from(), q.to());
        List<MemberFollowUpItem> followUps = jdbc.query("SELECT m.id,m.full_name,DATEDIFF(CURRENT_DATE,MAX(c.check_in_at)) days_since FROM member_profile m LEFT JOIN gym_check_in c ON c.tenant_id=m.tenant_id AND c.member_id=m.id WHERE m.tenant_id=? AND m.status='ACTIVE' GROUP BY m.id,m.full_name HAVING MAX(c.check_in_at) IS NULL OR MAX(c.check_in_at) < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY) ORDER BY days_since DESC LIMIT 20", (rs, row) -> new MemberFollowUpItem(rs.getLong("id"), rs.getString("full_name"), "连续30天未到店", rs.getInt("days_since")), tenantId);
        return new OperationsMetricsContext(q.from(), q.to(), q.periodLabel(), visitors, checkIns, activeMembers, previousVisitors, previousCheckIns, changeRate(visitors, previousVisitors), bookings, completed, bookings == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(bookings), 1, RoundingMode.HALF_UP), expiring7, expiring30, inactive, workoutMembers, workoutCount, followUps);
    }

    private long scalar(String sql, Object... args) {
        Number value = jdbc.queryForObject(sql, Number.class, args);
        return value == null ? 0 : value.longValue();
    }

    private String format(OperationsMetricsContext m) {
        StringBuilder out = new StringBuilder("【门店运营数据，统计周期：").append(m.periodLabel()).append("】\n")
                .append("到店会员数：").append(m.uniqueVisitors()).append("人；签到次数：").append(m.checkInCount()).append("次\n")
                .append("上期到店会员数：").append(m.previousUniqueVisitors()).append("人；变化率：").append(m.visitorChangeRate()).append("%\n")
                .append("当前有效会员：").append(m.activeMembers()).append("人；预约：").append(m.bookings()).append("次；已到店预约：").append(m.completedBookings()).append("次；预约到店率：").append(m.attendanceRate()).append("%\n")
                .append("7天内到期：").append(m.expiringWithin7Days()).append("人；30天内到期：").append(m.expiringWithin30Days()).append("人\n")
                .append("连续30天未到店：").append(m.inactiveMembers()).append("人；训练会员：").append(m.workoutMembers()).append("人；训练次数：").append(m.workoutCount()).append("次");
        if (!m.followUps().isEmpty()) {
            out.append("\n需要跟进的会员（最多20人）：");
            m.followUps().forEach(item -> out.append(" ").append(item.memberName()).append("（").append(item.daysSinceLastCheckIn()).append("天未到店）；"));
        }
        return out.toString();
    }

    private List<String> groupedCheckIns(Long tenantId, OperationsMetricsQuery query,
                                         OperationsGroupBy groupBy, List<OperationsMetric> metrics) {
        if (metrics.stream().noneMatch(Set.of(OperationsMetric.UNIQUE_VISITORS, OperationsMetric.CHECK_IN_COUNT)::contains)) {
            return List.of();
        }
        String expression = switch (groupBy) {
            case DAY -> "DATE(check_in_at)";
            case WEEK -> "YEARWEEK(check_in_at, 1)";
            case MONTH -> "DATE_FORMAT(check_in_at, '%Y-%m')";
            case NONE -> throw new IllegalArgumentException("不支持的分组维度");
        };
        String sql = "SELECT " + expression + " AS bucket, COUNT(DISTINCT member_id) AS visitors, COUNT(*) AS check_ins "
                + "FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ? GROUP BY " + expression
                + " ORDER BY MIN(check_in_at)";
        return jdbc.query(sql, (rs, row) -> {
            StringBuilder line = new StringBuilder(rs.getString("bucket"));
            if (metrics.contains(OperationsMetric.UNIQUE_VISITORS)) line.append(" 到店会员").append(rs.getLong("visitors")).append("人");
            if (metrics.contains(OperationsMetric.CHECK_IN_COUNT)) line.append(" 签到").append(rs.getLong("check_ins")).append("次");
            return line.toString();
        }, tenantId, query.from(), query.to());
    }

    private String formatStructured(OperationsMetricsContext m, OperationsQueryIntent intent, List<String> grouped) {
        Set<OperationsMetric> selected = intent.metrics().isEmpty()
                ? EnumSet.allOf(OperationsMetric.class)
                : EnumSet.copyOf(intent.metrics());
        StringBuilder out = new StringBuilder("【门店运营数据，统计周期：").append(m.periodLabel()).append("】");
        if (selected.contains(OperationsMetric.UNIQUE_VISITORS)) out.append("\n到店会员数：").append(m.uniqueVisitors()).append("人");
        if (selected.contains(OperationsMetric.CHECK_IN_COUNT)) out.append("\n签到次数：").append(m.checkInCount()).append("次");
        if (selected.contains(OperationsMetric.ACTIVE_MEMBERS)) out.append("\n当前有效会员：").append(m.activeMembers()).append("人");
        if (selected.contains(OperationsMetric.BOOKINGS)) out.append("\n预约数：").append(m.bookings()).append("次");
        if (selected.contains(OperationsMetric.ATTENDANCE_RATE)) out.append("\n预约到店率：").append(m.attendanceRate()).append("%");
        if (selected.contains(OperationsMetric.EXPIRING_MEMBERS)) out.append("\n30天内到期会员：").append(m.expiringWithin30Days()).append("人");
        if (selected.contains(OperationsMetric.INACTIVE_MEMBERS)) out.append("\n连续30天未到店：").append(m.inactiveMembers()).append("人");
        if (selected.contains(OperationsMetric.WORKOUT_MEMBERS)) out.append("\n训练会员：").append(m.workoutMembers()).append("人");
        if (selected.contains(OperationsMetric.WORKOUT_COUNT)) out.append("\n训练次数：").append(m.workoutCount()).append("次");
        if (intent.comparePrevious() && selected.contains(OperationsMetric.UNIQUE_VISITORS)) {
            out.append("\n上期到店会员数：").append(m.previousUniqueVisitors()).append("人；变化率：").append(m.visitorChangeRate()).append("%");
        }
        if (!grouped.isEmpty()) {
            out.append("\n分组维度：").append(intent.groupBy().name()).append("\n分组结果：");
            grouped.forEach(value -> out.append("\n- ").append(value));
        }
        return out.toString();
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw new IllegalArgumentException(name + " 必须是字符串");
        return value.asText();
    }

    private static <E extends Enum<E>> E enumValue(JsonNode node, String name, Class<E> type, E fallback) {
        String value = text(node, name);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的" + name + "：" + value);
        }
    }
}
