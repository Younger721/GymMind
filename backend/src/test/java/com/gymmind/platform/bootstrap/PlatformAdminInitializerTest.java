package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PlatformAdminInitializerTest {
    @Test
    void existingPasswordIsNotReplacedUnlessForceSyncIsEnabled() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        UserRoleRepository userRoles = mock(UserRoleRepository.class);
        DatabaseInitializationLock lock = mock(DatabaseInitializationLock.class);
        PlatformAdminProperties properties = new PlatformAdminProperties();
        properties.setEmail("admin@example.com");
        properties.setUsername("admin");
        properties.setPassword("new-password");
        Role role = Role.of(RoleCode.PLATFORM_ADMIN, "Platform administrator");
        UserAccount user = UserAccount.platformAdmin("admin@example.com", "admin", new BCryptPasswordEncoder().encode("old-password"), "Admin");
        when(roles.findByCode(RoleCode.PLATFORM_ADMIN)).thenReturn(Optional.of(role));
        when(users.findByNormalizedEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(userRoles.existsByUserIdAndRoleId(any(), any())).thenReturn(true);
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(1)).run(); return null; }).when(lock).execute(anyString(), any());

        new PlatformAdminInitializer(users, roles, userRoles, properties, lock).run(mock(ApplicationArguments.class));

        verify(users, never()).save(any());
        assertThat(new BCryptPasswordEncoder().matches("old-password", user.getPasswordHash())).isTrue();
    }
}
