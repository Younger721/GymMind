package com.gymmind.analytics.application;

import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.analytics.domain.*;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WeeklyReportServiceTest {
    @Test void generatesModelReportFromTenantScopedWeeklyMetrics() {
        var metrics = metrics();
        var weekly = mock(WeeklyMetricsService.class);
        var model = mock(ChatModelGateway.class);
        when(weekly.weekly(any(), eq(LocalDate.of(2026, 9, 7)))).thenReturn(metrics);
        when(model.complete(any(), any())).thenReturn("本周训练完成度良好");
        var service = new DefaultWeeklyReportService(weekly, model);

        var report = service.report(actor(7L), LocalDate.of(2026, 9, 7));

        assertThat(report.weekStart()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(report.summary()).isEqualTo("本周训练完成度良好");
        verify(model).complete(contains("会员数=20"), any());
    }

    @Test void returnsDeterministicFallbackWhenModelFails() {
        var weekly = mock(WeeklyMetricsService.class);
        var model = mock(ChatModelGateway.class);
        when(weekly.weekly(any(), any())).thenReturn(metrics());
        when(model.complete(any(), any())).thenThrow(new RuntimeException("timeout"));
        var report = new DefaultWeeklyReportService(weekly, model).report(actor(7L), LocalDate.of(2026, 9, 7));
        assertThat(report.summary()).contains("本周共 8 次预约");
    }

    private WeeklyMetrics metrics() {
        return new WeeklyMetrics(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13),
                new DashboardMetrics(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), 20, 16,
                        new BigDecimal("0.8000"), 8, new BigDecimal("0.7500"), 6,
                        new BigDecimal("0.7500"), new BigDecimal("0"), new BigDecimal("0.5000")));
    }
    private CurrentActor actor(long tenant) { return new CurrentActor(1L, tenant, Set.of(RoleCode.MEMBER), Set.of("ai:report"), 0, "t"); }
}
