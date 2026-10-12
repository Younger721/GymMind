package com.gymmind.analytics.application;

import com.gymmind.ai.harness.plugins.MemberFollowUpItem;
import com.gymmind.ai.harness.plugins.OperationsMetricsContext;
import com.gymmind.ai.harness.plugins.OperationsMetricsQuery;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class DefaultTenantOperationsQueryService implements TenantOperationsQueryService {

    private final JdbcTemplate jdbc;

    public DefaultTenantOperationsQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void requireAccess(CurrentActor actor) {
        if (actor == null || !actor.hasPermission("analytics:read")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (ActorAccess.tenantId(actor) == null) {
            throw new BusinessException(ErrorCode.TENANT_CONTEXT_REQUIRED);
        }
        if (!actor.roles().contains(RoleCode.GYM_ADMIN) && !actor.isPlatformAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @Override
    public OperationsMetricsContext queryMetrics(Long tenantId, OperationsMetricsQuery query) {
        validateRange(query.from(), query.to());
        LocalDate previousFrom = query.from().minusMonths(1);
        LocalDate previousTo = query.to().minusMonths(1);
        if (query.from().getDayOfMonth() == 1
                && query.to().equals(query.from().plusMonths(1).minusDays(1))) {
            previousTo = previousFrom.withDayOfMonth(previousFrom.lengthOfMonth());
        }
        long visitors = scalar(
                "SELECT COUNT(DISTINCT member_id) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?",
                tenantId, query.from(), query.to());
        long checkIns = scalar(
                "SELECT COUNT(*) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?",
                tenantId, query.from(), query.to());
        long previousVisitors = scalar(
                "SELECT COUNT(DISTINCT member_id) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?",
                tenantId, previousFrom, previousTo);
        long previousCheckIns = scalar(
                "SELECT COUNT(*) FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ?",
                tenantId, previousFrom, previousTo);
        long activeMembers = scalar("SELECT COUNT(*) FROM member_profile WHERE tenant_id=? AND status='ACTIVE'", tenantId);
        long bookings = scalar(
                "SELECT COUNT(*) FROM course_booking WHERE tenant_id=? AND DATE(created_at) BETWEEN ? AND ? AND status <> 'CANCELLED'",
                tenantId, query.from(), query.to());
        long completed = scalar(
                "SELECT COUNT(*) FROM course_booking WHERE tenant_id=? AND DATE(checked_in_at) BETWEEN ? AND ? AND status='COMPLETED'",
                tenantId, query.from(), query.to());
        long expiring7 = scalar(
                "SELECT COUNT(DISTINCT member_id) FROM member_membership WHERE tenant_id=? AND status='ACTIVE' AND expires_at >= CURRENT_TIMESTAMP AND expires_at < DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY)",
                tenantId);
        long expiring30 = scalar(
                "SELECT COUNT(DISTINCT member_id) FROM member_membership WHERE tenant_id=? AND status='ACTIVE' AND expires_at >= CURRENT_TIMESTAMP AND expires_at < DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)",
                tenantId);
        long inactive = scalar(
                "SELECT COUNT(*) FROM member_profile m WHERE m.tenant_id=? AND m.status='ACTIVE' AND NOT EXISTS (SELECT 1 FROM gym_check_in c WHERE c.tenant_id=m.tenant_id AND c.member_id=m.id AND c.check_in_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY))",
                tenantId);
        long workoutMembers = scalar(
                "SELECT COUNT(DISTINCT member_id) FROM workout_record WHERE tenant_id=? AND workout_date BETWEEN ? AND ? AND status='COMPLETED'",
                tenantId, query.from(), query.to());
        long workoutCount = scalar(
                "SELECT COUNT(*) FROM workout_record WHERE tenant_id=? AND workout_date BETWEEN ? AND ? AND status='COMPLETED'",
                tenantId, query.from(), query.to());
        List<MemberFollowUpItem> followUps = jdbc.query(
                "SELECT m.id,m.full_name,DATEDIFF(CURRENT_DATE,MAX(c.check_in_at)) days_since FROM member_profile m LEFT JOIN gym_check_in c ON c.tenant_id=m.tenant_id AND c.member_id=m.id WHERE m.tenant_id=? AND m.status='ACTIVE' GROUP BY m.id,m.full_name HAVING MAX(c.check_in_at) IS NULL OR MAX(c.check_in_at) < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY) ORDER BY days_since DESC LIMIT 20",
                (rs, row) -> new MemberFollowUpItem(rs.getLong("id"), rs.getString("full_name"), "连续30天未到店", rs.getInt("days_since")),
                tenantId);
        BigDecimal attendanceRate = bookings == 0
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(bookings), 1, RoundingMode.HALF_UP);
        return new OperationsMetricsContext(
                query.from(),
                query.to(),
                query.periodLabel(),
                visitors,
                checkIns,
                activeMembers,
                previousVisitors,
                previousCheckIns,
                TenantOperationsMath.changeRate(visitors, previousVisitors),
                bookings,
                completed,
                attendanceRate,
                expiring7,
                expiring30,
                inactive,
                workoutMembers,
                workoutCount,
                followUps);
    }

    @Override
    public TenantOperationsSummaryView summary(Long tenantId, OperationsMetricsQuery query, boolean comparePrevious) {
        OperationsMetricsContext metrics = queryMetrics(tenantId, query);
        long totalMembers = scalar("SELECT COUNT(*) FROM member_profile WHERE tenant_id=?", tenantId);
        Long prevVisitors = comparePrevious ? metrics.previousUniqueVisitors() : null;
        Long prevCheckIns = comparePrevious ? metrics.previousCheckInCount() : null;
        BigDecimal visitorRate = comparePrevious
                ? metrics.visitorChangeRate()
                : null;
        BigDecimal checkInRate = comparePrevious
                ? TenantOperationsMath.changeRate(metrics.checkInCount(), metrics.previousCheckInCount())
                : null;
        return new TenantOperationsSummaryView(
                metrics.from(),
                metrics.to(),
                metrics.periodLabel(),
                totalMembers,
                metrics.activeMembers(),
                metrics.uniqueVisitors(),
                metrics.checkInCount(),
                prevVisitors,
                prevCheckIns,
                visitorRate,
                checkInRate);
    }

    @Override
    public List<TenantOperationsTrendPoint> trend(Long tenantId, LocalDate from, LocalDate to, String groupBy) {
        validateRange(from, to);
        String normalized = groupBy == null || groupBy.isBlank()
                ? "day"
                : groupBy.trim().toLowerCase(Locale.ROOT);
        String expression = switch (normalized) {
            case "day" -> "DATE(check_in_at)";
            case "week" -> "YEARWEEK(check_in_at, 1)";
            case "month" -> "DATE_FORMAT(check_in_at, '%Y-%m')";
            default -> throw new IllegalArgumentException("不支持的分组维度：" + groupBy);
        };
        String sql = "SELECT " + expression + " AS bucket, COUNT(DISTINCT member_id) AS visitors, COUNT(*) AS check_ins "
                + "FROM gym_check_in WHERE tenant_id=? AND DATE(check_in_at) BETWEEN ? AND ? GROUP BY " + expression
                + " ORDER BY MIN(check_in_at)";
        return jdbc.query(sql, (rs, row) -> {
            String bucket = rs.getString("bucket");
            LocalDate day = "day".equals(normalized) && bucket != null
                    ? LocalDate.parse(bucket)
                    : null;
            return new TenantOperationsTrendPoint(
                    bucket,
                    day,
                    rs.getLong("visitors"),
                    rs.getLong("check_ins"));
        }, tenantId, from, to);
    }

    static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Invalid date range");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new IllegalArgumentException("查询时间范围不能超过 366 天");
        }
    }

    private long scalar(String sql, Object... args) {
        Number value = jdbc.queryForObject(sql, Number.class, args);
        return value == null ? 0 : value.longValue();
    }
}
