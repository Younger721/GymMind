package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

@Component
@Order(100)
public class SystemCatalogInitializer implements ApplicationRunner {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final DatabaseInitializationLock initializationLock;

    public SystemCatalogInitializer(RoleRepository roleRepository, PermissionRepository permissionRepository,
                                    DatabaseInitializationLock initializationLock) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.initializationLock = initializationLock;
    }

    public void run() {
        initializationLock.execute("gymmind:system-catalog", this::initialize);
    }

    @Override
    public void run(ApplicationArguments args) {
        run();
    }

    private void initialize() {
        for (RoleCode code : RoleCode.values()) {
            roleRepository.findByCode(code).orElseGet(() -> roleRepository.save(Role.of(code, displayName(code))));
        }
        for (PermissionCatalog.Entry entry : PermissionCatalog.entries()) {
            permissionRepository.findByCode(entry.code())
                    .orElseGet(() -> permissionRepository.save(Permission.of(entry.code(), entry.name())));
        }
    }

    private static String displayName(RoleCode code) {
        return switch (code) {
            case PLATFORM_ADMIN -> "Platform administrator";
            case GYM_ADMIN -> "Gym administrator";
            case COACH -> "Coach";
            case MEMBER -> "Member";
        };
    }
}
