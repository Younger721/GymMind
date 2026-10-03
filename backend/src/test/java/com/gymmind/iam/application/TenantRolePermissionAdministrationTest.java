package com.gymmind.iam.application;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.TenantRolePermission;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.TenantRolePermissionRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.platform.bootstrap.PermissionCatalog;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static com.gymmind.support.SecurityTestActors.tenantActor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantRolePermissionAdministrationTest {

    @Test
    void gymAdminWritesMenuOverrideForCurrentTenant() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        UserRoleRepository userRoles = mock(UserRoleRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository globalLinks = mock(RolePermissionRepository.class);
        TenantRolePermissionRepository tenantLinks = mock(TenantRolePermissionRepository.class);
        Role coach = role(3L, RoleCode.COACH);
        when(roles.findByCode(RoleCode.COACH)).thenReturn(Optional.of(coach));
        when(permissions.findAll()).thenReturn(catalogPermissions());

        DefaultUserAdministrationService service = service(users, userRoles, roles, permissions, globalLinks, tenantLinks);
        service.replaceRolePermissions(gymAdmin(), RoleCode.COACH, Set.of("menu:training"));

        verify(tenantLinks).deleteByTenantIdAndRoleId(10L, 3L);
        ArgumentCaptor<TenantRolePermission> saved = ArgumentCaptor.forClass(TenantRolePermission.class);
        verify(tenantLinks, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(link -> link.getPermission().getCode())
                .contains("menu:training", PermissionCatalog.MENU_CONFIGURED_MARKER);
        verify(globalLinks, never()).deleteByRoleId(anyLong());
    }

    @Test
    void gymAdminCannotChangeItsOwnRoleMenu() {
        DefaultUserAdministrationService service = service(mock(UserAccountRepository.class),
                mock(UserRoleRepository.class), mock(RoleRepository.class), mock(PermissionRepository.class),
                mock(RolePermissionRepository.class), mock(TenantRolePermissionRepository.class));

        assertThatThrownBy(() -> service.replaceRolePermissions(gymAdmin(), RoleCode.GYM_ADMIN, Set.of("menu:admin")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void emptyTenantOverrideStillPersistsConfiguredMarker() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        UserRoleRepository userRoles = mock(UserRoleRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository globalLinks = mock(RolePermissionRepository.class);
        TenantRolePermissionRepository tenantLinks = mock(TenantRolePermissionRepository.class);
        Role member = role(4L, RoleCode.MEMBER);
        when(roles.findByCode(RoleCode.MEMBER)).thenReturn(Optional.of(member));
        when(permissions.findAll()).thenReturn(catalogPermissions());

        DefaultUserAdministrationService service = service(users, userRoles, roles, permissions, globalLinks, tenantLinks);
        service.replaceRolePermissions(gymAdmin(), RoleCode.MEMBER, Set.of());

        ArgumentCaptor<TenantRolePermission> saved = ArgumentCaptor.forClass(TenantRolePermission.class);
        verify(tenantLinks).save(saved.capture());
        assertThat(saved.getValue().getPermission().getCode())
                .isEqualTo(PermissionCatalog.MENU_CONFIGURED_MARKER);
    }

    @Test
    void nonAdminCannotReadOrWriteRoleMenus() {
        DefaultUserAdministrationService service = service(mock(UserAccountRepository.class),
                mock(UserRoleRepository.class), mock(RoleRepository.class), mock(PermissionRepository.class),
                mock(RolePermissionRepository.class), mock(TenantRolePermissionRepository.class));
        CurrentActor coach = tenantActor(10L, Set.of(RoleCode.COACH), Set.of());

        assertThatThrownBy(() -> service.listPermissionCatalog(coach))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> service.replaceRolePermissions(coach, RoleCode.MEMBER, Set.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    private static DefaultUserAdministrationService service(UserAccountRepository users,
                                                             UserRoleRepository userRoles,
                                                             RoleRepository roles,
                                                             PermissionRepository permissions,
                                                             RolePermissionRepository globalLinks,
                                                             TenantRolePermissionRepository tenantLinks) {
        TenantRepository tenants = mock(TenantRepository.class);
        Tenant tenant = Tenant.create("test-tenant", "Test Tenant");
        ReflectionTestUtils.setField(tenant, "id", 10L);
        when(tenants.findById(10L)).thenReturn(Optional.of(tenant));
        return new DefaultUserAdministrationService(users, userRoles, roles, permissions, globalLinks,
                tenantLinks, tenants, null, null, null);
    }

    private static List<Permission> catalogPermissions() {
        List<Permission> result = new ArrayList<>();
        PermissionCatalog.entries().stream().map(entry -> Permission.of(entry.code(), entry.name())).forEach(result::add);
        PermissionCatalog.menuEntries().stream().map(entry -> Permission.of(entry.code(), entry.name())).forEach(result::add);
        result.add(Permission.of(PermissionCatalog.MENU_CONFIGURED_MARKER,
                PermissionCatalog.MENU_CONFIGURED_MARKER_NAME));
        long id = 1;
        for (Permission permission : result) ReflectionTestUtils.setField(permission, "id", id++);
        return result;
    }

    private static Role role(Long id, RoleCode code) {
        Role role = Role.of(code, code.name());
        ReflectionTestUtils.setField(role, "id", id);
        return role;
    }
}
