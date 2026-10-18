package com.gymmind.iam.application;

import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.platform.bootstrap.PermissionCatalog;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdministrationServiceTest {

    private UserAccountRepository users;
    private UserRoleRepository userRoles;
    private RoleRepository roles;
    private UserAdministrationService service;

    private static final CurrentActor ADMIN = new CurrentActor(
            2L, 10L, Set.of(RoleCode.GYM_ADMIN),
            Set.of("user:read", "user:write", "role:read", "role:assign"), 3L, "admin-token");

    @BeforeEach
    void setUp() {
        users = mock(UserAccountRepository.class);
        userRoles = mock(UserRoleRepository.class);
        roles = mock(RoleRepository.class);
        service = new DefaultUserAdministrationService(users, userRoles, roles);
        TenantContextHolder.clear();
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void gymAdminCreatesOnlyTenantCoachOrMemberWithHashedPassword() {
        UserAccount saved = UserAccount.tenantUser(10L, "coach@example.com", "$2a$10$hash", "Coach", RoleCode.COACH);
        ReflectionTestUtils.setField(saved, "id", 20L);
        Role coach = role(2L, RoleCode.COACH);
        when(users.save(any())).thenReturn(saved);
        when(roles.findByCode(RoleCode.COACH)).thenReturn(Optional.of(coach));

        when(users.findByNormalizedUsername(any())).thenReturn(Optional.empty());
        UserAdministrationService.UserView result = service.create(ADMIN,
                new CreateUserCommand("coach", " Coach@Example.COM ", "password123", "Coach", RoleCode.COACH));

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.tenantId()).isEqualTo(10L);
        verify(users).save(any(UserAccount.class));
        verify(userRoles).save(any());
    }

    @Test
    void platformAdminCannotCreateTenantUserThroughGenericUserAdministration() {
        TenantContextHolder.set(10L);
        assertThatThrownBy(() -> service.create(platformAdmin(),
                new CreateUserCommand("gym-admin", "admin@example.com", "password123", "Gym Admin",
                        RoleCode.GYM_ADMIN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).save(any());
    }

    @Test
    void platformAdminCannotReplaceTenantRolesThroughGenericUserAdministration() {
        TenantContextHolder.set(10L);
        assertThatThrownBy(() -> service.replaceRoles(platformAdmin(), 21L, Set.of(RoleCode.COACH)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).findByTenantIdAndId(any(), any());
    }

    @Test
    void gymAdminCannotCreateAnotherGymAdmin() {
        assertThatThrownBy(() -> service.create(ADMIN,
                new CreateUserCommand("other-admin", "other-admin@example.com", "password123", "Other Admin",
                        RoleCode.GYM_ADMIN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).save(any());
    }

    @Test
    void gymAdminCannotGrantPlatformOrGymAdminRole() {
        assertThatThrownBy(() -> service.replaceRoles(ADMIN, 20L, Set.of(RoleCode.PLATFORM_ADMIN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> service.replaceRoles(ADMIN, 20L, Set.of(RoleCode.GYM_ADMIN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).findByTenantIdAndId(any(), any());
    }

    @Test
    void crossTenantUserIsHiddenAsNotFound() {
        when(users.findByTenantIdAndId(10L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(ADMIN, 99L, UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void statusAndRoleChangesIncrementTokenVersion() {
        UserAccount user = UserAccount.tenantUser(10L, "member@example.com", "hash", "Member", RoleCode.MEMBER);
        ReflectionTestUtils.setField(user, "id", 20L);
        Role coach = role(2L, RoleCode.COACH);
        when(users.findByTenantIdAndId(10L, 20L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        when(roles.findByCode(RoleCode.COACH)).thenReturn(Optional.of(coach));

        service.changeStatus(ADMIN, 20L, UserStatus.DISABLED);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        service.replaceRoles(ADMIN, 20L, Set.of(RoleCode.COACH));
        assertThat(user.getTokenVersion()).isEqualTo(2L);
    }

    @Test
    void gymAdminCannotManageAnotherGymAdmin() {
        UserAccount otherAdmin = UserAccount.tenantUser(10L, "other-admin@example.com", "hash", "Admin", RoleCode.GYM_ADMIN);
        ReflectionTestUtils.setField(otherAdmin, "id", 21L);
        when(users.findByTenantIdAndId(10L, 21L)).thenReturn(Optional.of(otherAdmin));
        when(userRoles.findRoleCodesByUserId(21L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));

        assertThatThrownBy(() -> service.changeStatus(ADMIN, 21L, UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void platformAdminCannotChangeStatusThroughGenericUserAdministration() {
        TenantContextHolder.set(10L);
        UserAccount gymAdmin = UserAccount.tenantUser(10L, "gymadmin@example.com", "hash", "Admin", RoleCode.GYM_ADMIN);
        ReflectionTestUtils.setField(gymAdmin, "id", 21L);
        when(users.findByTenantIdAndId(10L, 21L)).thenReturn(Optional.of(gymAdmin));
        when(userRoles.findRoleCodesByUserId(21L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(users.save(gymAdmin)).thenReturn(gymAdmin);

        assertThatThrownBy(() -> service.changeStatus(platformAdmin(), 21L, UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).save(any());
    }

    @Test
    void listIsTenantScopedAndReturnsPageEnvelope() {
        UserAccount user = UserAccount.tenantUser(10L, "member@example.com", "hash", "Member", RoleCode.MEMBER);
        ReflectionTestUtils.setField(user, "id", 20L);
        when(users.findAllByTenantId(10L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        PageResponse<UserAdministrationService.UserSummary> page = service.list(ADMIN, PageRequest.of(0, 20));

        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.content()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(20L);
            assertThat(summary.tenantId()).isEqualTo(10L);
        });
        verify(users).findAllByTenantId(10L, PageRequest.of(0, 20));
        verify(users, never()).findByTenantIdOrTenantIdIsNullOrderByIdAsc(any(), any());
    }

    @Test
    void platformAdminListsTheSelectedTenantUsers() {
        TenantContextHolder.set(10L);
        when(users.findAllByTenantIdAndRoleCode(10L, RoleCode.GYM_ADMIN, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<UserAdministrationService.UserSummary> page =
                service.list(platformAdmin(), PageRequest.of(0, 20));

        assertThat(page.totalElements()).isZero();
        verify(users).findAllByTenantIdAndRoleCode(10L, RoleCode.GYM_ADMIN, PageRequest.of(0, 20));
        verify(users, never()).findByTenantIdOrTenantIdIsNullOrderByIdAsc(any(), any());
    }

    @Test
    void platformAdminCannotListUsersWithoutSelectedTenant() {
        assertThatThrownBy(() -> service.list(platformAdmin(), PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).findByTenantIdOrTenantIdIsNullOrderByIdAsc(any(), any());
    }

    @Test
    void rolePermissionCatalogContainsMenusOnlyAndCountsOnlyMenuLinks() {
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        Role platform = role(1L, RoleCode.PLATFORM_ADMIN);
        Role gymAdmin = role(2L, RoleCode.GYM_ADMIN);
        Role coach = role(3L, RoleCode.COACH);
        Role member = role(4L, RoleCode.MEMBER);
        when(roles.findAll()).thenReturn(List.of(platform, gymAdmin, coach, member));
        when(links.findPermissionCodesByRoleId(1L)).thenReturn(Set.of("menu:dashboard", "user:read", "menu:admin"));
        when(links.findPermissionCodesByRoleId(2L)).thenReturn(Set.of("menu:operations", "member:read"));
        when(links.findPermissionCodesByRoleId(3L)).thenReturn(Set.of());
        when(links.findPermissionCodesByRoleId(4L)).thenReturn(Set.of());

        UserAdministrationService roleService = new DefaultUserAdministrationService(users, userRoles, roles,
                permissions, links);
        List<UserAdministrationService.PermissionView> catalog = roleService.listPermissionCatalog(platformActor());
        List<UserAdministrationService.RoleView> roleViews = roleService.listRoles(platformActor());

        assertThat(catalog).extracting(UserAdministrationService.PermissionView::code)
                .containsExactlyInAnyOrderElementsOf(PermissionCatalog.menuEntries().stream()
                        .map(PermissionCatalog.MenuEntry::code).toList());
        assertThat(catalog).allSatisfy(item -> assertThat(item.code()).startsWith("menu:"));
        assertThat(catalog.stream().filter(item -> item.code().equals("menu:admin")).findFirst().orElseThrow().roles())
                .containsExactly(RoleCode.PLATFORM_ADMIN);
        assertThat(roleViews).extracting(UserAdministrationService.RoleView::code)
                .containsExactlyInAnyOrder(RoleCode.PLATFORM_ADMIN, RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER);
        assertThat(roleViews.stream().filter(role -> role.code() == RoleCode.GYM_ADMIN).findFirst().orElseThrow()
                .permissionCount()).isEqualTo(1);
        verify(permissions, never()).findAll();
    }

    @Test
    void roleMenuUpdateExpandsMenusToOperationPermissions() {
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        Role gymAdmin = role(2L, RoleCode.GYM_ADMIN);
        when(roles.findByCode(RoleCode.GYM_ADMIN)).thenReturn(Optional.of(gymAdmin));
        when(permissions.findAll()).thenReturn(catalogPermissions());

        UserAdministrationService roleService = new DefaultUserAdministrationService(users, userRoles, roles,
                permissions, links);
        roleService.replaceRolePermissions(platformActor(), RoleCode.GYM_ADMIN, Set.of("menu:operations"));

        verify(links).deleteByRoleId(2L);
        verify(links, times(PermissionCatalog.operationPermissionsFor("menu:operations").size() + 2)).save(any());
        org.mockito.ArgumentCaptor<RolePermission> saved = org.mockito.ArgumentCaptor.forClass(RolePermission.class);
        verify(links, times(PermissionCatalog.operationPermissionsFor("menu:operations").size() + 2)).save(saved.capture());
        Set<String> savedCodes = saved.getAllValues().stream()
                .map(link -> ((Permission) ReflectionTestUtils.getField(link, "permission")).getCode())
                .collect(java.util.stream.Collectors.toSet());
        assertThat(savedCodes).contains("menu:operations")
                .containsAll(PermissionCatalog.operationPermissionsFor("menu:operations"));
    }

    @Test
    void roleMenuUpdateRejectsOperationPermissionCodesBeforeReplacingLinks() {
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        when(roles.findByCode(RoleCode.GYM_ADMIN)).thenReturn(Optional.of(role(2L, RoleCode.GYM_ADMIN)));
        UserAdministrationService roleService = new DefaultUserAdministrationService(users, userRoles, roles,
                permissions, links);

        assertThatThrownBy(() -> roleService.replaceRolePermissions(
                platformActor(), RoleCode.GYM_ADMIN, Set.of("user:write")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
        verify(permissions, never()).findAll();
        verify(links, never()).deleteByRoleId(anyLong());
    }

    @Test
    void roleMenuUpdateRejectsMenusOutsideRoleBusinessBoundary() {
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        when(roles.findByCode(RoleCode.COACH)).thenReturn(Optional.of(role(3L, RoleCode.COACH)));
        UserAdministrationService roleService = new DefaultUserAdministrationService(users, userRoles, roles,
                permissions, links);

        assertThatThrownBy(() -> roleService.replaceRolePermissions(
                platformActor(), RoleCode.COACH, Set.of("menu:admin")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
        verify(permissions, never()).findAll();
        verify(links, never()).deleteByRoleId(anyLong());
    }

    @Test
    void platformAdminRoleMenuUpdateIsRejected() {
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        UserAdministrationService roleService = new DefaultUserAdministrationService(users, userRoles, roles,
                permissions, links);

        assertThatThrownBy(() -> roleService.replaceRolePermissions(
                platformActor(), RoleCode.PLATFORM_ADMIN, Set.of("menu:admin")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(roles, never()).findByCode(RoleCode.PLATFORM_ADMIN);
        verify(links, never()).deleteByRoleId(anyLong());
    }

    private static List<Permission> catalogPermissions() {
        List<Permission> result = PermissionCatalog.entries().stream()
                .map(entry -> Permission.of(entry.code(), entry.name())).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        PermissionCatalog.menuEntries().stream().map(entry -> Permission.of(entry.code(), entry.name())).forEach(result::add);
        result.add(Permission.of(PermissionCatalog.MENU_CONFIGURED_MARKER,
                PermissionCatalog.MENU_CONFIGURED_MARKER_NAME));
        return result;
    }

    private static CurrentActor platformActor() {
        return new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform-test-token");
    }

    private static Role role(Long id, RoleCode code) {
        Role role = Role.of(code, code.name());
        ReflectionTestUtils.setField(role, "id", id);
        return role;
    }
}
