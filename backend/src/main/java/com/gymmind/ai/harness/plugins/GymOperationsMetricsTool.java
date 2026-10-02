package com.gymmind.ai.harness.plugins;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 门店运营数据工具：固定白名单 SQL，模型永远不能传入任意 SQL。 */
@Component
public class GymOperationsMetricsTool implements AiHarnessPlugin {
    private static final AiHarnessPluginDescriptor DESCRIPTOR = new AiHarnessPluginDescriptor(
            "gym_operations_metrics", "门店运营数据", "查询当前健身房签到、预约、会籍和训练活跃数据", "TOOL", "analytics:read", true, true);
    private static final String QUESTION_KEYWORDS = "到店|签到|来了|活跃|预约|到店率|会籍|续费|到期|训练次数|训练活跃|会员减少|会员增加|经营|运营|趋势";
    private final JdbcTemplate jdbc;
    private final Clock clock;

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

    private OperationsMetricsQuery resolveQuery(String input, LocalDate today) {
        if (input != null && (input.contains("最近7天") || input.contains("近7天"))) {
            LocalDate first = today.minusDays(6);
            return new OperationsMetricsQuery(first, today, first + "至" + today);
        }
        if (input != null && (input.contains("最近30天") || input.contains("近30天"))) {
            LocalDate first = today.minusDays(29);
            return new OperationsMetricsQuery(first, today, first + "至" + today);
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
}
