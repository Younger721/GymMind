package com.gymmind.iam.application;

import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.platform.bootstrap.PermissionCatalog;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DefaultUserAdministrationService implements UserAdministrationService {

    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public DefaultUserAdministrationService(UserAccountRepository users,
                                             UserRoleRepository userRoles,
                                             RoleRepository roles,
                                             PermissionRepository permissions,
                                             RolePermissionRepository rolePermissions) {
        this.users = Objects.requireNonNull(users, "users");
        this.userRoles = Objects.requireNonNull(userRoles, "userRoles");
        this.roles = Objects.requireNonNull(roles, "roles");
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
    }

    public DefaultUserAdministrationService(UserAccountRepository users, UserRoleRepository userRoles, RoleRepository roles) {
        this(users, userRoles, roles, null, null);
    }

    @Override
    @Transactional
    public UserView create(CurrentActor actor, CreateUserCommand command) {
        Long tenantId = requireGymAdmin(actor, "user:write");
        Objects.requireNonNull(command, "command");
        RoleCode roleCode = requireTenantRole(actor, command.role());
        String email = UserAccount.normalizeEmail(command.email());
        String username = UserAccount.normalizeUsername(required(command.username(), "Username"));
        if (users.findByNormalizedEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (users.findByNormalizedUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        UserAccount user = UserAccount.tenantUser(tenantId, email, username,
                passwordEncoder.encode(required(command.password(), "Password")),
                required(command.displayName(), "Display name"), roleCode);
        UserAccount saved = users.save(user);
        Role role = roles.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        if (!role.isActive()) throw new BusinessException(ErrorCode.FORBIDDEN);
        userRoles.save(UserRole.assign(saved, role));
        return view(saved, Set.of(roleCode));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummary> list(CurrentActor actor, Pageable pageable) {
        if (ActorAccess.isSuperAdmin(actor)) {
            Pageable effective = pageable == null ? Pageable.ofSize(20) : pageable;
            Long tenantId = ActorAccess.tenantId(actor);
            Page<UserAccount> page = tenantId == null
                    ? users.findAll(effective)
                    : users.findAllByTenantId(tenantId, effective);
            return new PageResponse<>(page.map(this::summary).getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
        Long tenantId = requireGymAdmin(actor, "user:read");
        Pageable effective = pageable == null ? Pageable.ofSize(20) : pageable;
        Page<UserAccount> page = users.findAllByTenantId(tenantId, effective);
        return new PageResponse<>(page.map(this::summary).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public void changeStatus(CurrentActor actor, Long userId, UserStatus status) {
        Long tenantId = requireGymAdmin(actor, "user:write");
        if (status == null || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        UserAccount user = findTenantUser(tenantId, userId);
        requireManagedUser(actor, userId);
        if (status == UserStatus.ACTIVE) {
            user.activate();
        } else {
            user.disable();
        }
        users.save(user);
    }

    @Override
    @Transactional
    public void replaceRoles(CurrentActor actor, Long userId, Set<RoleCode> roleCodes) {
        Long tenantId = requireGymAdmin(actor, "role:assign");
        boolean superAdmin = ActorAccess.isSuperAdmin(actor);
        if (userId == null || userId <= 0 || roleCodes == null || roleCodes.isEmpty()
                || roleCodes.stream().anyMatch(code -> code == null || code == RoleCode.PLATFORM_ADMIN
                || (!superAdmin && code == RoleCode.GYM_ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        UserAccount user = findTenantUser(tenantId, userId);
        requireManagedUser(actor, userId);
        Set<RoleCode> current = userRoles.findRoleCodesByUserId(userId);
        if (!Objects.equals(current, roleCodes)) {
            RoleCode primary = roleCodes.stream().sorted(Comparator.comparing(Enum::name)).findFirst().orElseThrow();
            user.changeRole(primary);
            userRoles.deleteByUserId(userId);
            for (RoleCode code : roleCodes) {
                Role role = roles.findByCode(code)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
                if (!role.isActive()) throw new BusinessException(ErrorCode.FORBIDDEN);
                userRoles.save(UserRole.assign(user, role));
            }
            users.save(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleView> listRoles(CurrentActor actor) {
        if (!ActorAccess.isSuperAdmin(actor)) requireGymAdmin(actor, "role:read");
        return roles.findAll().stream()
                .map(role -> new RoleView(role.getId(), role.getCode(), role.getName(), role.getStatus().name(),
                        rolePermissions == null ? 0 : menuPermissionCount(role.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionView> listPermissionCatalog(CurrentActor actor) {
        requirePlatformPermission(actor, "role:permission:read");
        List<Role> roleList = roles.findAll();
        if (permissions == null || rolePermissions == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        Map<String, Set<RoleCode>> roleCodesByMenu = new HashMap<>();
        for (Role role : roleList) {
            Set<String> assigned = rolePermissions.findPermissionCodesByRoleId(role.getId());
            if (assigned == null) assigned = Set.of();
            for (String code : assigned) {
                if (PermissionCatalog.isMenuPermission(code)) {
                    roleCodesByMenu.computeIfAbsent(code, ignored -> new java.util.HashSet<>()).add(role.getCode());
                }
            }
        }
        return PermissionCatalog.menuEntries().stream()
                .map(entry -> new PermissionView(entry.code(), entry.name(),
                        Set.copyOf(roleCodesByMenu.getOrDefault(entry.code(), Set.of()))))
                .toList();
    }

    @Override
    @Transactional
    public void replaceRolePermissions(CurrentActor actor, RoleCode roleCode, Set<String> permissionCodes) {
        requirePlatformPermission(actor, "role:permission:write");
        if (roleCode == null || roleCode == RoleCode.PLATFORM_ADMIN || permissionCodes == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (permissions == null || rolePermissions == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        Role role = roles.findByCode(roleCode).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Set<String> normalized = permissionCodes.stream().filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isBlank()).collect(Collectors.toSet());
        if (normalized.stream().anyMatch(code -> !PermissionCatalog.isMenuPermission(code)
                || !PermissionCatalog.isMenuAllowedForRole(roleCode, code))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        Map<String, Permission> catalog = permissions.findAll().stream()
                .collect(Collectors.toMap(Permission::getCode, permission -> permission, (left, right) -> left));
        if (normalized.stream().anyMatch(code -> !catalog.containsKey(code))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        Permission configuredMarker = catalog.get(PermissionCatalog.MENU_CONFIGURED_MARKER);
        if (configuredMarker == null) {
            configuredMarker = permissions.findByCode(PermissionCatalog.MENU_CONFIGURED_MARKER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        }
        rolePermissions.deleteByRoleId(role.getId());
        Set<String> linked = new java.util.HashSet<>();
        for (String menuCode : normalized) {
            linkPermission(role, catalog, linked, menuCode);
            for (String operationCode : PermissionCatalog.operationPermissionsFor(roleCode, menuCode)) {
                linkPermission(role, catalog, linked, operationCode);
            }
        }
        rolePermissions.save(RolePermission.link(role, configuredMarker));
    }

    private int menuPermissionCount(Long roleId) {
        Set<String> assigned = rolePermissions.findPermissionCodesByRoleId(roleId);
        if (assigned == null) return 0;
        return (int) assigned.stream().filter(PermissionCatalog::isMenuPermission).count();
    }

    private void linkPermission(Role role, Map<String, Permission> catalog, Set<String> linked, String code) {
        if (!linked.add(code)) return;
        Permission permission = catalog.get(code);
        if (permission == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        rolePermissions.save(RolePermission.link(role, permission));
    }

    @Override
    @Transactional
    public void changeRoleStatus(CurrentActor actor, RoleCode roleCode, boolean active) {
        requirePlatformPermission(actor, "role:write");
        if (roleCode == null || roleCode == RoleCode.PLATFORM_ADMIN) throw new BusinessException(ErrorCode.FORBIDDEN);
        Role role = roles.findByCode(roleCode).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (active) role.activate(); else role.disable();
        roles.save(role);
    }

    private static void requirePlatformPermission(CurrentActor actor, String permission) {
        if (actor == null || !actor.isPlatformAdmin() || !actor.hasPermission(permission)) throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private UserAccount findTenantUser(Long tenantId, Long userId) {
        return users.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /** 场馆管理员不能操作其他管理员；平台管理员不受此限制 */
    private void requireManagedUser(CurrentActor actor, Long userId) {
        if (ActorAccess.isSuperAdmin(actor)) {
            return;
        }
        Set<RoleCode> currentRoles = userRoles.findRoleCodesByUserId(userId);
        if (currentRoles != null && currentRoles.stream()
                .anyMatch(code -> code == RoleCode.PLATFORM_ADMIN || code == RoleCode.GYM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static Long requireGymAdmin(CurrentActor actor, String permission) {
        ActorAccess.requireGymAdmin(actor, permission);
        return ActorAccess.requireTenantId(actor);
    }

    private static RoleCode requireTenantRole(CurrentActor actor, RoleCode role) {
        boolean platformAdmin = ActorAccess.isSuperAdmin(actor);
        if (role == RoleCode.GYM_ADMIN && platformAdmin) {
            return role;
        }
        if (role != RoleCode.COACH && role != RoleCode.MEMBER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return role;
    }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, label + " must not be blank");
        }
        return value.trim();
    }

    private UserSummary summary(UserAccount user) {
        return new UserSummary(user.getId(), user.getTenantId(), user.getNormalizedUsername(),
                user.getNormalizedEmail(), user.getDisplayName(), user.getStatus(),
                userRoles.findRoleCodesByUserId(user.getId()), null, null);
    }

    private UserView view(UserAccount user, Set<RoleCode> roleCodes) {
        return new UserView(user.getId(), user.getTenantId(), user.getNormalizedUsername(),
                user.getNormalizedEmail(), user.getDisplayName(), user.getStatus(), roleCodes);
    }
}
