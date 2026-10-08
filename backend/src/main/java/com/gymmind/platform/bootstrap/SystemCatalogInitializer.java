package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Order(100)
public class SystemCatalogInitializer implements ApplicationRunner {
    private static final String REPAIR_PERMISSION_SQL = """
            UPDATE sys_permission
               SET name = CASE WHEN name IS NULL OR TRIM(name) = '' THEN ? ELSE name END,
                   created_at = CASE
                       WHEN created_at IS NULL OR CAST(created_at AS CHAR) LIKE '0000-00-00%'
                       THEN CURRENT_TIMESTAMP ELSE created_at END,
                   updated_at = CASE
                       WHEN updated_at IS NULL OR CAST(updated_at AS CHAR) LIKE '0000-00-00%'
                       THEN CURRENT_TIMESTAMP ELSE updated_at END
             WHERE code = ?
               AND (name IS NULL OR TRIM(name) = ''
                    OR created_at IS NULL OR updated_at IS NULL
                    OR CAST(created_at AS CHAR) LIKE '0000-00-00%'
                    OR CAST(updated_at AS CHAR) LIKE '0000-00-00%')
            """;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final DatabaseInitializationLock initializationLock;
    private final RolePermissionRepository rolePermissionRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Constructor retained for unit tests and legacy callers without a JDBC handle. */
    public SystemCatalogInitializer(RoleRepository roleRepository, PermissionRepository permissionRepository,
                                    DatabaseInitializationLock initializationLock,
                                    RolePermissionRepository rolePermissionRepository) {
        this(roleRepository, permissionRepository, initializationLock, rolePermissionRepository, null);
    }

    @Autowired
    public SystemCatalogInitializer(RoleRepository roleRepository, PermissionRepository permissionRepository,
                                    DatabaseInitializationLock initializationLock,
                                    RolePermissionRepository rolePermissionRepository,
                                    JdbcTemplate jdbcTemplate) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.initializationLock = initializationLock;
        this.rolePermissionRepository = rolePermissionRepository;
        this.jdbcTemplate = jdbcTemplate;
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

        // Repair with SQL before findByCode/findAll can ask Hibernate to hydrate
        // a historical MySQL zero-date timestamp.
        for (PermissionCatalog.Entry entry : PermissionCatalog.entries()) {
            repairPermissionRow(entry.code(), entry.name());
        }
        for (PermissionCatalog.MenuEntry entry : PermissionCatalog.menuEntries()) {
            repairPermissionRow(entry.code(), entry.name());
        }
        repairPermissionRow(PermissionCatalog.MENU_CONFIGURED_MARKER,
                PermissionCatalog.MENU_CONFIGURED_MARKER_NAME);

        Map<String, Permission> permissions = new HashMap<>();
        for (PermissionCatalog.Entry entry : PermissionCatalog.entries()) {
            permissions.put(entry.code(), permissionRepository.findByCode(entry.code())
                    .orElseGet(() -> permissionRepository.save(Permission.of(entry.code(), entry.name()))));
        }
        for (PermissionCatalog.MenuEntry entry : PermissionCatalog.menuEntries()) {
            permissions.put(entry.code(), permissionRepository.findByCode(entry.code())
                    .orElseGet(() -> permissionRepository.save(Permission.of(entry.code(), entry.name()))));
        }
        permissions.put(PermissionCatalog.MENU_CONFIGURED_MARKER,
                permissionRepository.findByCode(PermissionCatalog.MENU_CONFIGURED_MARKER)
                        .orElseGet(() -> permissionRepository.save(Permission.of(
                                PermissionCatalog.MENU_CONFIGURED_MARKER,
                                PermissionCatalog.MENU_CONFIGURED_MARKER_NAME))));

        Map<RoleCode, Role> roles = new HashMap<>();
        for (RoleCode code : RoleCode.values()) {
            roles.put(code, roleRepository.findByCode(code).orElseThrow());
        }

        // Platform administrators are always linked to every catalog item and
        // are intentionally not editable from the role permission screen.
        for (PermissionCatalog.Entry entry : PermissionCatalog.entries()) {
            link(roles.get(RoleCode.PLATFORM_ADMIN), permissions.get(entry.code()));
        }
        for (PermissionCatalog.MenuEntry entry : PermissionCatalog.menuEntries()) {
            link(roles.get(RoleCode.PLATFORM_ADMIN), permissions.get(entry.code()));
        }

        for (RoleCode roleCode : Set.of(RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER)) {
            seedTenantRole(roles.get(roleCode), permissions);
        }
    }

    private void repairPermissionRow(String code, String name) {
        if (jdbcTemplate != null) {
            jdbcTemplate.update(REPAIR_PERMISSION_SQL, name, code);
        }
        // Keeps repository implementations that provide a native repair hook
        // useful while the JDBC path guarantees the first entity read is safe.
        permissionRepository.repairMalformedEntry(code, name);
    }

    private void seedTenantRole(Role role, Map<String, Permission> permissions) {
        Set<String> existing = rolePermissionRepository.findPermissionCodesByRoleId(role.getId());
        if (existing == null) existing = Set.of();
        boolean menuConfigurationExists = existing.contains(PermissionCatalog.MENU_CONFIGURED_MARKER);
        Set<String> configuredMenus = new HashSet<>();
        for (String code : existing) {
            if (PermissionCatalog.isMenuPermission(code)
                    && PermissionCatalog.isMenuAllowedForRole(role.getCode(), code)) {
                configuredMenus.add(code);
            }
        }
        if (!menuConfigurationExists && configuredMenus.isEmpty()) {
            configuredMenus.addAll(PermissionCatalog.defaultMenus(role.getCode()));
        }
        // Keep the tenant administrator's configuration entry available after
        // splitting it from the shared assistant menu, including old installs.
        if (role.getCode() == RoleCode.GYM_ADMIN) {
            configuredMenus.add(PermissionCatalog.AI_CONFIG_MENU);
        }

        // Old installations only had operation links. Add the selected menu
        // links and their complete operation expansion without deleting any
        // existing links, so tenant customizations survive a restart.
        for (String menuCode : configuredMenus) {
            link(role, permissions.get(menuCode));
            for (String operationCode : PermissionCatalog.operationPermissionsFor(role.getCode(), menuCode)) {
                link(role, permissions.get(operationCode));
            }
        }
        link(role, permissions.get(PermissionCatalog.MENU_CONFIGURED_MARKER));
    }

    private void link(Role role, Permission permission) {
        if (role == null || permission == null) throw new IllegalStateException("Missing catalog role or permission");
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
