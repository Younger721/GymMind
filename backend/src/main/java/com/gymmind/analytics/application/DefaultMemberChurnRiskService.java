package com.gymmind.analytics.application;

import com.gymmind.analytics.domain.ChurnRiskCalculator;
import com.gymmind.analytics.domain.ChurnSignals;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DefaultMemberChurnRiskService implements MemberChurnRiskService {

    private final JdbcTemplate jdbc;
    private final ChurnRiskCalculator calculator = new ChurnRiskCalculator();

    public DefaultMemberChurnRiskService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MemberChurnRiskView> list(CurrentActor actor) {
        Long tenant = ActorAccess.tenantId(actor);
        if (tenant == null || !actor.hasPermission("analytics:read")
                || !actor.roles().contains(RoleCode.GYM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Instant now = Instant.now();
        Instant from30 = now.minus(Duration.ofDays(30));
        Instant from60 = now.minus(Duration.ofDays(60));
        List<Map<String, Object>> members = jdbc.queryForList(
                "select id, full_name from member_profile where tenant_id=? and status='ACTIVE'", tenant);
        List<MemberChurnRiskView> out = new ArrayList<>();
        for (Map<String, Object> member : members) {
            Long id = ((Number) member.get("id")).longValue();
            Instant lastCheckIn = jdbc.query(
                    "select check_in_at from gym_check_in where tenant_id=? and member_id=? order by check_in_at desc limit 1",
                    rs -> rs.next() ? rs.getTimestamp("check_in_at").toInstant() : null,
                    tenant, id);
            int checkInsLast30 = countCheckIns(tenant, id, from30, null);
            int checkInsPrevious30 = countCheckIns(tenant, id, from60, from30);
            Integer bookings = jdbc.queryForObject(
                    "select count(*) from course_booking where tenant_id=? and member_id=? and created_at>=? and status<>'CANCELLED'",
                    Integer.class, tenant, id, from30);
            Integer completed = jdbc.queryForObject(
                    "select count(*) from course_booking where tenant_id=? and member_id=? and checked_in_at>=? and status='COMPLETED'",
                    Integer.class, tenant, id, from30);
            Integer daysRemaining = jdbc.queryForObject(
                    "select coalesce(datediff(expires_at, current_timestamp), 9999) from member_membership where tenant_id=? and member_id=? and status='ACTIVE' order by expires_at limit 1",
                    Integer.class, tenant, id);
            int remaining = daysRemaining == null ? 9999 : Math.max(daysRemaining, 0);
            int since = lastCheckIn == null ? 999 : Math.max(0, (int) Duration.between(lastCheckIn, now).toDays());
            boolean expiring = remaining <= 14;
            var risk = calculator.calculate(new ChurnSignals(
                    since,
                    checkInsLast30,
                    checkInsPrevious30,
                    bookings == null ? 0 : bookings,
                    completed == null ? 0 : completed,
                    remaining,
                    expiring));
            out.add(new MemberChurnRiskView(
                    id,
                    String.valueOf(member.get("full_name")),
                    lastCheckIn,
                    checkInsLast30,
                    bookings == null ? 0 : bookings,
                    completed == null ? 0 : completed,
                    remaining,
                    risk.level(),
                    risk.score(),
                    risk.reasons()));
        }
        return out.stream().sorted(Comparator.comparing(MemberChurnRiskView::score).reversed()).toList();
    }

    private int countCheckIns(Long tenantId, Long memberId, Instant from, Instant toExclusive) {
        if (toExclusive == null) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from gym_check_in where tenant_id=? and member_id=? and check_in_at>=?",
                    Integer.class, tenantId, memberId, from);
            return count == null ? 0 : count;
        }
        Integer count = jdbc.queryForObject(
                "select count(*) from gym_check_in where tenant_id=? and member_id=? and check_in_at>=? and check_in_at<?",
                Integer.class, tenantId, memberId, from, toExclusive);
        return count == null ? 0 : count;
    }
}
