package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
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
    private final RolePermissionRepository rolePermissionRepository;

    public SystemCatalogInitializer(RoleRepository roleRepository, PermissionRepository permissionRepository,
                                    DatabaseInitializationLock initializationLock,
                                    RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.initializationLock = initializationLock;
        this.rolePermissionRepository = rolePermissionRepository;
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
        link(RoleCode.PLATFORM_ADMIN, "platform:tenant:read");
        link(RoleCode.PLATFORM_ADMIN, "platform:tenant:write");
        link(RoleCode.GYM_ADMIN, "tenant:settings:read");
        link(RoleCode.GYM_ADMIN, "tenant:settings:write");
        link(RoleCode.GYM_ADMIN, "user:read");
        link(RoleCode.GYM_ADMIN, "user:write");
        link(RoleCode.GYM_ADMIN, "role:read");
        link(RoleCode.GYM_ADMIN, "role:assign");
        link(RoleCode.GYM_ADMIN, "member:read");
        link(RoleCode.GYM_ADMIN, "member:write");
        link(RoleCode.COACH, "coach:read");
        link(RoleCode.MEMBER, "member:read");
        link(RoleCode.GYM_ADMIN, "course:read");
        link(RoleCode.GYM_ADMIN, "course:write");
        link(RoleCode.GYM_ADMIN, "membership:read");
        link(RoleCode.GYM_ADMIN, "membership:write");
        link(RoleCode.GYM_ADMIN, "payment:write");
        link(RoleCode.GYM_ADMIN, "order:write");
    }

    private void link(RoleCode roleCode, String permissionCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        Permission permission = permissionRepository.findByCode(permissionCode).orElseThrow();
        if (!rolePermissionRepository.exists(role.getId(), permission.getId())) {
            rolePermissionRepository.save(RolePermission.link(role, permission));
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
