package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.membership.domain.model.MembershipPackage;
import com.gymmind.membership.domain.repository.MembershipPackageRepository;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MembershipPackageServiceTest {
    @Test
    void packageStoresBusinessFactsWithinActorTenant() {
        MembershipPackageRepository repository = mock(MembershipPackageRepository.class);
        when(repository.save(any(MembershipPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultMembershipPackageService service = new DefaultMembershipPackageService(repository, mock(AuditRecorder.class));
        MembershipPackageView view = service.create(admin(11L), new CreateMembershipPackageCommand(null, "10次卡", "COUNT", 10, null, new BigDecimal("199.00"), "CNY"));
        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.entitlementCount()).isEqualTo(10);
    }
    @Test
    void crossTenantPackageCannotBeRead() {
        MembershipPackageRepository repository = mock(MembershipPackageRepository.class);
        when(repository.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.empty());
        DefaultMembershipPackageService service = new DefaultMembershipPackageService(repository, mock(AuditRecorder.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.find(admin(11L), 8L)).isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }
    private static CurrentActor admin(long tenantId){return new CurrentActor(1L,tenantId, Set.of(RoleCode.GYM_ADMIN),Set.of("membership:write","membership:read"),0L,"token");}
}
