package com.gymmind.analytics.infrastructure;

import com.gymmind.analytics.domain.BookingMetricsPort;
import com.gymmind.analytics.domain.MemberMetricsPort;
import com.gymmind.analytics.domain.MembershipMetricsPort;
import com.gymmind.analytics.domain.PaymentMetricsPort;
import com.gymmind.booking.domain.model.BookingStatus;
import com.gymmind.member.domain.model.MemberStatus;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.membership.domain.model.MemberMembershipStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
class DefaultMemberMetricsPort implements MemberMetricsPort {
    private final MemberRepository members;

    DefaultMemberMetricsPort(MemberRepository members) {
        this.members = members;
    }

    @Override
    public long total(Long tenantId) {
        return members.countByTenantId(tenantId);
    }

    @Override
    public long active(Long tenantId) {
        return members.countByTenantIdAndStatus(tenantId, MemberStatus.ACTIVE);
    }
}

@Component
class DefaultBookingMetricsPort implements BookingMetricsPort {
    private final JdbcTemplate jdbc;

    DefaultBookingMetricsPort(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long total(Long tenantId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM course_booking WHERE tenant_id = ? AND created_at >= ? AND created_at < ? AND status <> ?",
                Long.class, tenantId, start, end, BookingStatus.CANCELLED.name());
        return count == null ? 0L : count;
    }

    @Override
    public long checkedIn(Long tenantId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM course_booking WHERE tenant_id = ? AND created_at >= ? AND created_at < ? AND status = ?",
                Long.class, tenantId, start, end, BookingStatus.COMPLETED.name());
        return count == null ? 0L : count;
    }
}

@Component
class DefaultPaymentMetricsPort implements PaymentMetricsPort {
    private final JdbcTemplate jdbc;

    DefaultPaymentMetricsPort(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BigDecimal amount(Long tenantId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payment_record WHERE tenant_id = ? AND recorded_at >= ? AND recorded_at < ?",
                BigDecimal.class, tenantId, start, end);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}

@Component
class DefaultMembershipMetricsPort implements MembershipMetricsPort {
    private final JdbcTemplate jdbc;

    DefaultMembershipMetricsPort(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long renewals(Long tenantId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM member_membership WHERE tenant_id = ? AND created_at >= ? AND created_at < ?",
                Long.class, tenantId, start, end);
        return count == null ? 0L : count;
    }

    @Override
    public long renewalCandidates(Long tenantId, LocalDate from, LocalDate to) {
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM member_membership WHERE tenant_id = ? AND status = ? AND expires_at IS NOT NULL AND expires_at < ?",
                Long.class, tenantId, MemberMembershipStatus.ACTIVE.name(), end);
        return count == null ? 0L : count;
    }
}
