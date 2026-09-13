package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemCatalogInitializerTest {

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

        new SystemCatalogInitializer(roles, permissions, lock, links).run();

        verify(links, org.mockito.Mockito.times(35)).save(any());
    }
}
