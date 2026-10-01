package com.gymmind.audit.application;

import com.gymmind.ai.application.AiUsageRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiUsageQueryServiceTest {

    @Test
    void platformAdminWithoutTenantReadsAllUsage() {
        AiUsageRepository repository = mock(AiUsageRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        var service = new DefaultAiUsageQueryService(repository);

        service.list(new CurrentActor(7L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "token"));

        verify(repository).findAll();
    }

    @Test
    void tenantAdminUsesTenantScopeWithoutPermissionCode() {
        AiUsageRepository repository = mock(AiUsageRepository.class);
        var service = new DefaultAiUsageQueryService(repository);

        service.list(new CurrentActor(7L, 11L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token"));

        verify(repository).findAllByTenantId(11L);
    }
}
