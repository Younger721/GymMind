package com.gymmind.coach.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.coach.domain.model.Coach;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoachServiceTest {
    @Test
    void adminCreatesCoachInCurrentTenant() {
        CoachRepository repository = mock(CoachRepository.class);
        when(repository.save(any(Coach.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultCoachService service = new DefaultCoachService(repository, mock(AuditRecorder.class));
        CoachView view = service.create(admin(11L), new CreateCoachCommand(null, "C-1", "Bob", "13900000000", 31L));
        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.userId()).isEqualTo(31L);
    }

    @Test
    void coachLookupIsTenantScoped() {
        CoachRepository repository = mock(CoachRepository.class);
        when(repository.findByTenantIdAndId(11L, 3L)).thenReturn(Optional.empty());
        DefaultCoachService service = new DefaultCoachService(repository, mock(AuditRecorder.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.find(admin(11L), 3L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        verify(repository).findByTenantIdAndId(11L, 3L);
    }
    private static CurrentActor admin(long tenantId) { return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("coach:read", "coach:write"), 0L, "token"); }
}
