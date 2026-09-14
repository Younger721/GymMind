package com.gymmind.audit.application;

import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.audit.domain.repository.OperationAuditRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.gymmind.shared.error.BusinessException;

class AuditQueryServiceTest {

    @Test
    void queriesOnlyCurrentTenant() {
        OperationAuditRepository repository = mock(OperationAuditRepository.class);
        var service = new DefaultAuditQueryService(repository);
        service.list(new CurrentActor(7L, 11L, Set.of(RoleCode.GYM_ADMIN), Set.of("audit:read"), 0L, "token"));
        verify(repository).findAllByTenantId(11L);
    }

    @Test
    void rejectsTenantActorWithoutAuditPermission() {
        OperationAuditRepository repository = mock(OperationAuditRepository.class);
        var service = new DefaultAuditQueryService(repository);
        assertThatThrownBy(() -> service.list(new CurrentActor(7L, 11L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token")))
                .isInstanceOf(BusinessException.class);
    }
}
