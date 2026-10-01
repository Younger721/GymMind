package com.gymmind.ai.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RecommendationServiceTest {
    private final CurrentActor actor = new CurrentActor(2L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:recommend"), 0, "t");

    @Test
    void rejectsRecommendationWhenAiModuleIsDisabled() {
        var quotas = mock(com.gymmind.tenancy.application.TenantQuotaService.class);
        var service = new DefaultRecommendationService(t -> List.of(), quotas);
        org.mockito.Mockito.doThrow(new com.gymmind.shared.error.BusinessException(com.gymmind.shared.error.ErrorCode.CONFLICT, "AI 模块未开通"))
                .when(quotas).ensureAiModuleEnabled(7L);
        assertThatThrownBy(() -> service.recommend(actor, "strength", Set.of()))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }

    @Test
    void filtersCandidatesWithHealthContraindicationsAndKeepsTenant() {
        var source = (RecommendationCandidateSource) tenant -> List.of(
                new RecommendationCandidate(1L, 7L, "深蹲", Set.of("strength"), Set.of("knee")),
                new RecommendationCandidate(2L, 7L, "平板支撑", Set.of("strength"), Set.of()),
                new RecommendationCandidate(3L, 8L, "外部动作", Set.of("strength"), Set.of()));
        var service = new DefaultRecommendationService(source, mock(com.gymmind.tenancy.application.TenantQuotaService.class));
        var result = service.recommend(actor, "strength", Set.of("knee"));
        assertThat(result).extracting(RecommendationCandidate::id).containsExactly(2L);
    }

    @Test
    void rejectsActorWithoutTenantOrPermission() {
        var service = new DefaultRecommendationService(t -> List.of(), mock(com.gymmind.tenancy.application.TenantQuotaService.class));
        var noPermission = new CurrentActor(2L, 7L, Set.of(RoleCode.MEMBER), Set.of(), 0, "t");
        assertThatThrownBy(() -> service.recommend(noPermission, "strength", Set.of()))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }
}
