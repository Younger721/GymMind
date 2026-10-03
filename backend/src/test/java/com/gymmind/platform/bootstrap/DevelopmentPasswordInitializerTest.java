package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentPasswordInitializerTest {

    @Test
    void existingDemoPasswordIsNotResetUnlessExplicitlyEnabled() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        String oldHash = new BCryptPasswordEncoder().encode("original-password");
        UserAccount user = UserAccount.platformAdmin("admin@gymmind.local", "admin", oldHash, "Admin");
        when(users.findAll()).thenReturn(List.of(user));

        DevelopmentPasswordInitializer initializer = new DevelopmentPasswordInitializer(users);
        ReflectionTestUtils.setField(initializer, "resetDemoPassword", false);
        initializer.run(mock(ApplicationArguments.class));

        verify(users, never()).save(any());
        assertThat(new BCryptPasswordEncoder().matches("original-password", user.getPasswordHash())).isTrue();
    }
}
