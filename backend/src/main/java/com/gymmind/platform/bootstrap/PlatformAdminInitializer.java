package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

@Component
@Order(200)
public class PlatformAdminInitializer implements ApplicationRunner {
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PlatformAdminProperties properties;
    private final DatabaseInitializationLock initializationLock;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlatformAdminInitializer(UserAccountRepository userRepository, RoleRepository roleRepository,
                                    UserRoleRepository userRoleRepository, PlatformAdminProperties properties,
                                    DatabaseInitializationLock initializationLock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.properties = properties;
        this.initializationLock = initializationLock;
    }

    public void run() {
        if (properties.isConfigured()) {
            initializationLock.execute("gymmind:platform-admin", this::initialize);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        run();
    }

    private void initialize() {
        Role role = roleRepository.findByCode(RoleCode.PLATFORM_ADMIN)
                .orElseThrow(() -> new IllegalStateException("PLATFORM_ADMIN role is not initialized"));
        UserAccount user = userRepository.findByNormalizedEmail(UserAccount.normalizeEmail(properties.getEmail()))
                .map(existing -> requirePlatformAccount(existing))
                .orElseGet(() -> userRepository.save(UserAccount.platformAdmin(
                        properties.getEmail(), passwordEncoder.encode(properties.getPassword()), "Platform administrator")));
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(UserRole.assign(user, role));
        }
    }

    private static UserAccount requirePlatformAccount(UserAccount user) {
        if (user.getTenantId() != null) {
            throw new IllegalStateException("Configured platform administrator email belongs to a tenant user");
        }
        return user;
    }
}
