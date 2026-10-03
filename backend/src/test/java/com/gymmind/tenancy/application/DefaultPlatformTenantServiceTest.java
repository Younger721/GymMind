package com.gymmind.tenancy.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.tenancy.application.command.CreateTenantCommand;
import com.gymmind.tenancy.application.command.UpdateTenantCommand;
import com.gymmind.tenancy.api.response.TenantView;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

        assertThat(user.getStatus()).isEqualTo(com.gymmind.iam.domain.model.UserStatus.DISABLED);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
    }

    @Test
    void updateChangesOnlyTenantName() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantProvisioningService provisioning = mock(TenantProvisioningService.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        Tenant tenant = Tenant.create("gym", "原名称");
        ReflectionTestUtils.setField(tenant, "id", 10L);
        when(tenants.findById(10L)).thenReturn(Optional.of(tenant));
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.countByTenantId(10L)).thenReturn(3L);
        DefaultPlatformTenantService service = new DefaultPlatformTenantService(tenants, provisioning, users);
        CurrentActor actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");

        TenantView view = service.update(10L, new UpdateTenantCommand("新名称"), actor);

        assertThat(view.name()).isEqualTo("新名称");
        assertThat(view.code()).isEqualTo("gym");
        assertThat(view.userCount()).isEqualTo(3L);
    }

    @Test
    void duplicateAdministratorEmailIsReportedAsConflict() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantProvisioningService provisioning = mock(TenantProvisioningService.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        when(users.findByNormalizedEmail("admin@example.com"))
                .thenReturn(Optional.of(UserAccount.platformAdmin("admin@example.com", "hash", "Admin")));
        DefaultPlatformTenantService service = new DefaultPlatformTenantService(tenants, provisioning, users);
        CurrentActor actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");

        assertThatThrownBy(() -> service.create(
                new CreateTenantCommand("New Gym", "admin@example.com", "password123", "Admin"), actor))
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void createsTenantWithGeneratedCode() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantProvisioningService provisioning = mock(TenantProvisioningService.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        when(tenants.existsByCode(anyString())).thenReturn(false);
        when(provisioning.provision(any(Tenant.class), any())).thenAnswer(invocation ->
                new TenantProvisioningService.ProvisionedTenant(invocation.getArgument(0), null));
        DefaultPlatformTenantService service = new DefaultPlatformTenantService(tenants, provisioning, users);
        CurrentActor actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");

        TenantView view = service.create(
                new CreateTenantCommand("New Gym", "admin@example.com", "password123", "Admin"), actor);

        assertThat(view.code()).matches("gym-[a-z0-9]{8}");
        verify(tenants).existsByCode(view.code());
    }

    @Test
    void retriesWhenGeneratedCodeAlreadyExists() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantProvisioningService provisioning = mock(TenantProvisioningService.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        when(tenants.existsByCode(anyString())).thenReturn(true, false);
        when(provisioning.provision(any(Tenant.class), any())).thenAnswer(invocation ->
                new TenantProvisioningService.ProvisionedTenant(invocation.getArgument(0), null));
        DefaultPlatformTenantService service = new DefaultPlatformTenantService(tenants, provisioning, users);
        CurrentActor actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");

        service.create(new CreateTenantCommand("New Gym", "admin@example.com", "password123", "Admin"), actor);

        verify(tenants, times(2)).existsByCode(anyString());
    }

    @Test
    void requiresAdministratorEmailAndPassword() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantProvisioningService provisioning = mock(TenantProvisioningService.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        DefaultPlatformTenantService service = new DefaultPlatformTenantService(tenants, provisioning, users);
        CurrentActor actor = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");

        assertThatThrownBy(() -> service.create(
                new CreateTenantCommand("New Gym", "", "password123", "Admin"), actor))
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThatThrownBy(() -> service.create(
                new CreateTenantCommand("New Gym", "admin@example.com", "", "Admin"), actor))
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
}
