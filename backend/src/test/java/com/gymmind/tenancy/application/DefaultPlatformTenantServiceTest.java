package com.gymmind.tenancy.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.tenancy.application.command.CreateTenantCommand;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPlatformTenantServiceTest {

    @Test
    void disablingTenantInvalidatesAllTenantUserTokensEvenAfterReactivation() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantProvisioningService provisioning = mock(TenantProvisioningService.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        Tenant tenant = Tenant.create("gym", "Gym");
        ReflectionTestUtils.setField(tenant, "id", 10L);
        UserAccount user = UserAccount.tenantUser(10L, "member@example.com", "hash", "Member", RoleCode.MEMBER);
        ReflectionTestUtils.setField(user, "id", 20L);
        when(tenants.findById(10L)).thenReturn(Optional.of(tenant));
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findAllByTenantId(10L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultPlatformTenantService service = new DefaultPlatformTenantService(tenants, provisioning, users);
        CurrentActor actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");

        service.disable(10L, actor);
        tenant.activate();

        assertThat(user.getTokenVersion()).isEqualTo(1L);
    }

    @Test
    void duplicateAdministratorEmailIsReportedAsConflict() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantProvisioningService provisioning = mock(TenantProvisioningService.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        when(tenants.existsByCode("new-gym")).thenReturn(false);
        when(users.findByNormalizedEmail("admin@example.com"))
                .thenReturn(Optional.of(UserAccount.platformAdmin("admin@example.com", "hash", "Admin")));
        DefaultPlatformTenantService service = new DefaultPlatformTenantService(tenants, provisioning, users);
        CurrentActor actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");

        assertThatThrownBy(() -> service.create(
                new CreateTenantCommand("new-gym", "New Gym", "admin@example.com", "password123", "Admin"), actor))
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
    }
}
