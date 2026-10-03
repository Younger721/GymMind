package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class SystemCatalogInitializerTest {

    @Test
    void catalogContainsPermissionsUsedByTenantScopedAiAndAuditApis() {
        assertThat(PermissionCatalog.entries()).extracting(PermissionCatalog.Entry::code)
                .contains("ai:recommend", "ai:report", "audit:read", "knowledge:write",
                        "ai:plan", "analytics:read", "knowledge:read",
                        "agent:write", "agent:chat", "platform:quota:write", "platform:stats:read",
                        "role:permission:read", "role:permission:write");
    }

    @Test
    void menuCatalogMapsOperationPermissionsAndDefinesBusinessDefaults() {
        assertThat(PermissionCatalog.menuEntries()).extracting(PermissionCatalog.MenuEntry::code)
                .containsExactlyInAnyOrder(
                        "menu:dashboard", "menu:assistant", "menu:knowledge", "menu:operations",
                        "menu:courses",
                        "menu:training", "menu:analytics", "menu:admin", "menu:tenant-settings",
                        "menu:platform", "menu:role-permissions", "menu:profile");
        assertThat(PermissionCatalog.menuPermissionFor("user:write")).contains("menu:admin");
        assertThat(PermissionCatalog.menuPermissionFor("workout:read")).contains("menu:training");
        assertThat(PermissionCatalog.menuPermissionFor("platform:tenant:read")).contains("menu:platform");
        assertThat(PermissionCatalog.operationPermissionsFor("menu:operations"))
                .contains("member:read", "coach:read")
                .doesNotContain("booking:write");
        assertThat(PermissionCatalog.operationPermissionsFor("menu:courses"))
                .contains("course:read", "course:write", "booking:write");
        assertThat(PermissionCatalog.defaultMenus(RoleCode.GYM_ADMIN))
                .containsExactlyInAnyOrder("menu:dashboard", "menu:assistant", "menu:knowledge",
                        "menu:operations", "menu:courses", "menu:training", "menu:analytics", "menu:admin",
                        "menu:tenant-settings", "menu:profile");
        assertThat(PermissionCatalog.defaultMenus(RoleCode.COACH))
                .containsExactlyInAnyOrder("menu:dashboard", "menu:assistant", "menu:knowledge",
                        "menu:courses", "menu:training", "menu:profile");
        assertThat(PermissionCatalog.defaultMenus(RoleCode.MEMBER))
                .containsExactlyInAnyOrder("menu:dashboard", "menu:assistant", "menu:courses", "menu:training", "menu:profile");
        assertThat(PermissionCatalog.isMenuAllowedForRole(RoleCode.GYM_ADMIN, "menu:operations")).isTrue();
        assertThat(PermissionCatalog.isMenuAllowedForRole(RoleCode.COACH, "menu:admin")).isFalse();
        assertThat(PermissionCatalog.isMenuAllowedForRole(RoleCode.MEMBER, "menu:platform")).isFalse();
    }

    @Test
    void roleMenuExpansionOnlyIncludesOperationsThatRoleCanUse() {
        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.GYM_ADMIN, "menu:assistant"))
                .contains("agent:write");
        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.COACH, "menu:assistant"))
                .contains("agent:chat")
                .doesNotContain("agent:write");
        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.MEMBER, "menu:assistant"))
                .contains("ai:chat", "ai:memory")
                .doesNotContain("agent:write");

        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.COACH, "menu:knowledge"))
                .contains("knowledge:read", "knowledge:write")
                .doesNotContain("search:write");
        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.MEMBER, "menu:knowledge"))
                .isEmpty();
        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.COACH, "menu:courses"))
                .contains("course:read", "course:write", "booking:write");
        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.MEMBER, "menu:courses"))
                .contains("course:read", "booking:write");

        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.COACH, "menu:training"))
                .contains("exercise:read", "video:read", "workout:write", "ai:plan")
                .doesNotContain("exercise:write", "video:write", "workout-record:write");
        assertThat(PermissionCatalog.operationPermissionsFor(RoleCode.MEMBER, "menu:training"))
                .contains("exercise:read", "video:read", "workout-record:write", "nutrition:write", "ai:plan")
                .doesNotContain("exercise:write", "video:write", "workout:write");
    }

    @Test
    void initializationLinksPlatformAndGymAdminPermissionsIdempotently() {
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        DatabaseInitializationLock lock = mock(DatabaseInitializationLock.class);
        Role platform = Role.of(RoleCode.PLATFORM_ADMIN, "Platform administrator");
        Role gymAdmin = Role.of(RoleCode.GYM_ADMIN, "Gym administrator");
        when(roles.findByCode(RoleCode.PLATFORM_ADMIN)).thenReturn(Optional.of(platform));
        when(roles.findByCode(RoleCode.GYM_ADMIN)).thenReturn(Optional.of(gymAdmin));
        when(roles.findByCode(RoleCode.COACH)).thenReturn(Optional.of(Role.of(RoleCode.COACH, "Coach")));
        when(roles.findByCode(RoleCode.MEMBER)).thenReturn(Optional.of(Role.of(RoleCode.MEMBER, "Member")));
        when(permissions.findByCode(any())).thenAnswer(invocation ->
                Optional.of(Permission.of((String) invocation.getArgument(0), (String) invocation.getArgument(0))));
        when(links.exists(any(), any())).thenReturn(false);
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(lock).execute(any(), any());

        SystemCatalogInitializer initializer = new SystemCatalogInitializer(roles, permissions, lock, links);
        initializer.run();
        verify(permissions, atLeastOnce()).repairMalformedEntry(any(), any());
        verify(links, atLeastOnce()).save(any());

        clearInvocations(links);
        when(links.exists(any(), any())).thenReturn(true);
        initializer.run();
        verify(links, never()).save(any());
    }

    @Test
    void configuredEmptyMenuSetRemainsEmptyAcrossInitializerRestart() {
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        DatabaseInitializationLock lock = mock(DatabaseInitializationLock.class);
        Map<RoleCode, Role> roleByCode = configureCatalog(roles, permissions, links, lock);
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.GYM_ADMIN).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER));
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.COACH).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER));
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.MEMBER).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER));

        SystemCatalogInitializer initializer = new SystemCatalogInitializer(roles, permissions, lock, links);
        initializer.run();

        ArgumentCaptor<RolePermission> saved = ArgumentCaptor.forClass(RolePermission.class);
        verify(links, atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).filteredOn(link ->
                link.getId().getRoleId().equals(roleByCode.get(RoleCode.GYM_ADMIN).getId()))
                .extracting(link -> permissionCode(link))
                .containsExactly(PermissionCatalog.MENU_CONFIGURED_MARKER);
    }

    @Test
    void existingRolePermissionLinksAreNotDeletedOnRestart() {
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        DatabaseInitializationLock lock = mock(DatabaseInitializationLock.class);
        Map<RoleCode, Role> roleByCode = configureCatalog(roles, permissions, links, lock);
        Set<String> existing = Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER, "menu:assistant", "custom:permission");
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.GYM_ADMIN).getId())).thenReturn(existing);
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.COACH).getId())).thenReturn(existing);
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.MEMBER).getId())).thenReturn(existing);

        new SystemCatalogInitializer(roles, permissions, lock, links).run();

        verify(links, never()).deleteByRoleId(anyLong());
    }

    @Test
    void configuredDisallowedMenuIsDroppedAtInitializerRestart() {
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        DatabaseInitializationLock lock = mock(DatabaseInitializationLock.class);
        Map<RoleCode, Role> roleByCode = configureCatalog(roles, permissions, links, lock);
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.GYM_ADMIN).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER, "menu:platform"));
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.COACH).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER));
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.MEMBER).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER));

        SystemCatalogInitializer initializer = new SystemCatalogInitializer(roles, permissions, lock, links);
        initializer.run();

        ArgumentCaptor<RolePermission> saved = ArgumentCaptor.forClass(RolePermission.class);
        verify(links, atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).filteredOn(link ->
                link.getId().getRoleId().equals(roleByCode.get(RoleCode.GYM_ADMIN).getId()))
                .extracting(this::permissionCode)
                .doesNotContain("menu:platform");
    }

    @Test
    void legacyOperationOnlyRoleIsMigratedToDefaultMenus() {
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RolePermissionRepository links = mock(RolePermissionRepository.class);
        DatabaseInitializationLock lock = mock(DatabaseInitializationLock.class);
        Map<RoleCode, Role> roleByCode = configureCatalog(roles, permissions, links, lock);
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.GYM_ADMIN).getId()))
                .thenReturn(Set.of("member:read"));
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.COACH).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER));
        when(links.findPermissionCodesByRoleId(roleByCode.get(RoleCode.MEMBER).getId()))
                .thenReturn(Set.of(PermissionCatalog.MENU_CONFIGURED_MARKER));

        SystemCatalogInitializer initializer = new SystemCatalogInitializer(roles, permissions, lock, links);
        initializer.run();

        ArgumentCaptor<RolePermission> saved = ArgumentCaptor.forClass(RolePermission.class);
        verify(links, atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).filteredOn(link ->
                link.getId().getRoleId().equals(roleByCode.get(RoleCode.GYM_ADMIN).getId()))
                .extracting(this::permissionCode)
                .containsAll(PermissionCatalog.defaultMenus(RoleCode.GYM_ADMIN))
                .contains(PermissionCatalog.MENU_CONFIGURED_MARKER);
    }

    private static Map<RoleCode, Role> configureCatalog(RoleRepository roles,
                                                         PermissionRepository permissions,
                                                         RolePermissionRepository links,
                                                         DatabaseInitializationLock lock) {
        Map<RoleCode, Role> roleByCode = new HashMap<>();
        long id = 1L;
        for (RoleCode code : RoleCode.values()) {
            Role role = Role.of(code, code.name());
            ReflectionTestUtils.setField(role, "id", id++);
            roleByCode.put(code, role);
            when(roles.findByCode(code)).thenReturn(Optional.of(role));
        }
        when(permissions.findByCode(any())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            return Optional.of(Permission.of(code, code));
        });
        when(links.exists(anyLong(), anyLong())).thenReturn(false);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(lock).execute(any(), any());
        return roleByCode;
    }

    private String permissionCode(RolePermission link) {
        Permission permission = (Permission) ReflectionTestUtils.getField(link, "permission");
        return permission.getCode();
    }
}
