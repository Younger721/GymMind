package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gymmind.analytics.application.MemberChurnRiskService;
import com.gymmind.analytics.application.MemberChurnRiskView;
import com.gymmind.analytics.domain.ChurnRiskLevel;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberChurnRiskCapabilityTest {

    @Test
    void gymAdminCanQueryFilteredRiskList() {
        var service = mock(MemberChurnRiskService.class);
        when(service.list(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new MemberChurnRiskView(1L, "张三", Instant.parse("2026-09-01T10:00:00Z"),
                        1, 0, 0, 5, ChurnRiskLevel.HIGH, 82, List.of("长期未到店")),
                new MemberChurnRiskView(2L, "李四", Instant.parse("2026-10-01T10:00:00Z"),
                        8, 2, 2, 60, ChurnRiskLevel.LOW, 10, List.of())));
        var capability = new MemberChurnRiskCapability(service);
        var actor = new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("analytics:read"), 0, "t");
        var args = JsonNodeFactory.instance.objectNode().put("minLevel", "MEDIUM").put("limit", 10);

        var result = capability.execute(actor, args, null);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("张三").contains("风险=高").contains("分数=82");
        assertThat(result.message()).doesNotContain("李四");
    }

    @Test
    void coachCannotUseChurnRiskTool() {
        var capability = new MemberChurnRiskCapability(mock(MemberChurnRiskService.class));
        var actor = new CurrentActor(1L, 7L, Set.of(RoleCode.COACH), Set.of("analytics:read"), 0, "t");
        assertThat(capability.supports(actor)).isFalse();
    }
}
