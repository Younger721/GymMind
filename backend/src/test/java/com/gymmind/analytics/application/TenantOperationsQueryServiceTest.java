package com.gymmind.analytics.application;

import com.gymmind.ai.harness.plugins.OperationsMetricsQuery;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantOperationsQueryServiceTest {

    @Test
    void rejectsPlatformActorWithoutTenantContext() {
        DefaultTenantOperationsQueryService service = new DefaultTenantOperationsQueryService(mock(JdbcTemplate.class));
        CurrentActor platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("analytics:read"), 0L, "token");

        assertThatThrownBy(() -> service.requireAccess(platform))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.TENANT_CONTEXT_REQUIRED);
    }

    @Test
    void rejectsDateRangeLongerThan366Days() {
        DefaultTenantOperationsQueryService service = new DefaultTenantOperationsQueryService(mock(JdbcTemplate.class));

        assertThatThrownBy(() -> service.queryMetrics(7L, new OperationsMetricsQuery(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 2, 1),
                "test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366");
    }

    @Test
    void summaryMapsCheckInPersonTimesFromMetrics() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), any(Class.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn(java.util.List.of());

        DefaultTenantOperationsQueryService service = new DefaultTenantOperationsQueryService(jdbc);
        OperationsMetricsQuery query = new OperationsMetricsQuery(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "2026年9月");
        TenantOperationsSummaryView view = service.summary(7L, query, true);

        assertThat(view.from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(view.checkInPersonTimes()).isZero();
        assertThat(view.activeMembers()).isZero();
    }
}
