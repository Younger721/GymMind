package com.gymmind.ai.harness.plugins;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.PlatformStatsService;
import com.gymmind.tenancy.application.PlatformStatsView;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformAiAdminCapabilityTest {

    @Test
    void platformAdminCanLoadSummary() {
        PlatformStatsService stats = mock(PlatformStatsService.class);
        when(stats.stats(any())).thenReturn(new PlatformStatsView(10, 8, 120, 6));
        var capability = new PlatformAiAdminCapability(stats, mock(TenantQuotaService.class));
        var actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:stats:read", "ai:chat"), 0, "t");

        assertThat(capability.supports(actor)).isTrue();
        var result = capability.execute(actor, null, null);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("本月全平台 AI 调用次数=120");
    }

    @Test
    void gymAdminCannotUsePlatformTool() {
        var capability = new PlatformAiAdminCapability(mock(PlatformStatsService.class), mock(TenantQuotaService.class));
        var actor = new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("platform:stats:read"), 0, "t");
        assertThat(capability.supports(actor)).isFalse();
    }
}
