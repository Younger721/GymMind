package com.gymmind.ai.application;

import com.gymmind.analytics.application.DashboardQueryService;
import com.gymmind.analytics.domain.DashboardMetrics;
import com.gymmind.analytics.domain.ChurnRiskLevel;
import com.gymmind.analytics.domain.ChurnRiskResult;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiAnalysisServiceTest {
    @Test
    void memberAnalysisUsesTenantActorAndModel() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(contains("训练频率"), anyList())).thenReturn("建议安排低强度恢复训练");
        var service = new DefaultAiAnalysisService(mock(DashboardQueryService.class), model);
        var result = service.memberAnalysis(actor(), new MemberAnalysisInput(18, 1, 8, 3, true));
        assertThat(result.summary()).isEqualTo("建议安排低强度恢复训练");
        verify(model).complete(contains("训练频率"), anyList());
    }

    @Test
    void operationAnalysisBuildsPromptFromTenantDashboard() {
        var model = mock(ChatModelGateway.class);
        when(model.complete(contains("会员数=10"), anyList())).thenReturn("活跃度下降，需要触达高风险会员");
        var dashboard = mock(DashboardQueryService.class);
        when(dashboard.dashboard(any(), any(), any())).thenReturn(new DashboardMetrics(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 10, 6,
                new BigDecimal("0.6"), 20, new BigDecimal("0.5"), 10,
                new BigDecimal("0.5"), new BigDecimal("1000"), new BigDecimal("0.4")));
        var service = new DefaultAiAnalysisService(dashboard, model);
        assertThat(service.operationAnalysis(actor(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).summary())
                .contains("活跃度下降");
    }

    @Test
    void platformActorIsRejected() {
        var service = new DefaultAiAnalysisService(mock(DashboardQueryService.class), mock(ChatModelGateway.class));
        assertThatThrownBy(() -> service.memberAnalysis(new CurrentActor(1L, null,
                Set.of(RoleCode.PLATFORM_ADMIN), Set.of("ai:analysis"), 0, "t"),
                new MemberAnalysisInput(1, 1, 1, 1, false))).isInstanceOf(RuntimeException.class);
    }

    private CurrentActor actor() {
        return new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:analysis"), 0, "t");
    }
}
