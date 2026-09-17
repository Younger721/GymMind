package com.gymmind.tenancy.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.TenantStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Service
public class DefaultPlatformStatsService implements PlatformStatsService {

    private final JdbcTemplate jdbc;

    public DefaultPlatformStatsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformStatsView stats(CurrentActor actor) {
        if (actor == null || !actor.isPlatformAdmin() || !actor.hasPermission("platform:stats:read")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        long total = count("SELECT COUNT(*) FROM sys_tenant");
        long active = count("SELECT COUNT(*) FROM sys_tenant WHERE status = ?", TenantStatus.ACTIVE.name());
        long aiEnabled = count("SELECT COUNT(*) FROM sys_tenant_quota WHERE ai_module_enabled = true");
        long aiCalls = countAiCallsThisMonth();
        return new PlatformStatsView(total, active, aiCalls, aiEnabled);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private long countAiCallsThisMonth() {
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return count("SELECT COUNT(*) FROM ai_usage_record WHERE created_at >= ? AND created_at < ?", start, end);
    }
}
