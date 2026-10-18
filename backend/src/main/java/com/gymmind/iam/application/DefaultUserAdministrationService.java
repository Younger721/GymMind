package com.gymmind.iam.application;

import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.model.TenantRolePermission;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.TenantRolePermissionRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.platform.bootstrap.PermissionCatalog;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MembershipPackage;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.membership.domain.repository.MembershipPackageRepository;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class DefaultUserAdministrationService implements UserAdministrationService {

    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final TenantRolePermissionRepository tenantRolePermissions;
    private final TenantRepository tenants;
    private final MemberRepository members;
    private final MemberMembershipRepository memberships;
    private final MembershipPackageRepository packages;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public DefaultUserAdministrationService(UserAccountRepository users,
                                             UserRoleRepository userRoles,
                                             RoleRepository roles,
                                             PermissionRepository permissions,
                                             RolePermissionRepository rolePermissions,
                                             TenantRolePermissionRepository tenantRolePermissions,
                                             TenantRepository tenants,
                                             MemberRepository members,
                                             MemberMembershipRepository memberships,
                                             MembershipPackageRepository packages) {
        this.users = Objects.requireNonNull(users, "users");
        this.userRoles = Objects.requireNonNull(userRoles, "userRoles");
        this.roles = Objects.requireNonNull(roles, "roles");
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
        this.tenantRolePermissions = tenantRolePermissions;
        this.tenants = tenants;
        this.members = members;
        this.memberships = memberships;
        this.packages = packages;
    }

    public DefaultUserAdministrationService(UserAccountRepository users, UserRoleRepository userRoles,
                                             RoleRepository roles, PermissionRepository permissions,
                                             RolePermissionRepository rolePermissions) {
        this(users, userRoles, roles, permissions, rolePermissions, null, null, null, null, null);
    }

    public DefaultUserAdministrationService(UserAccountRepository users, UserRoleRepository userRoles,
                                             RoleRepository roles, PermissionRepository permissions,
                                             RolePermissionRepository rolePermissions,
                                             TenantRolePermissionRepository tenantRolePermissions) {
        this(users, userRoles, roles, permissions, rolePermissions, tenantRolePermissions,
                null, null, null, null);
    }

    public DefaultUserAdministrationService(UserAccountRepository users, UserRoleRepository userRoles, RoleRepository roles) {
        this(users, userRoles, roles, null, null);
    }

    @Override
    @Transactional
    public UserView create(CurrentActor actor, CreateUserCommand command) {
        if (ActorAccess.isSuperAdmin(actor)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
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
                required(command.displayName(), "Display name"), command.phone(), roleCode);
        UserAccount saved = users.save(user);
        Role role = roles.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        if (!role.isActive()) throw new BusinessException(ErrorCode.FORBIDDEN);
        userRoles.save(UserRole.assign(saved, role));
        if (roleCode == RoleCode.MEMBER) {
            createMembership(saved, tenantId, command.membershipCardType());
        }
        return view(saved, Set.of(roleCode));
    }

    private void createMembership(UserAccount user, Long tenantId, String cardType) {
        Integer validDays = switch (cardType == null ? "" : cardType.trim()) {
            case "周卡" -> 7;
            case "月卡" -> 30;
            case "年卡" -> 365;
            default -> null;
        };
        if (validDays == null || members == null || memberships == null || packages == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "会员必须选择会员卡类型");
        }
        MembershipPackage membershipPackage = packages.findByTenantIdAndValidDays(tenantId, validDays)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "对应会员套餐不存在"));
        Member member = members.save(Member.create(tenantId, user.getId(), null, user.getDisplayName(), user.getPhone()));
        memberships.save(MemberMembership.create(tenantId, member.getId(), membershipPackage.getId(),
                membershipPackage.getEntitlementCount(), Instant.now(), Instant.now().plus(validDays, ChronoUnit.DAYS)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummary> list(CurrentActor actor, Pageable pageable) {
        if (ActorAccess.isSuperAdmin(actor)) {
            Pageable effective = pageable == null ? Pageable.ofSize(20) : pageable;
            Long tenantId = ActorAccess.tenantId(actor);
            if (tenantId == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            Page<UserAccount> page = users.findAllByTenantIdAndRoleCode(tenantId, RoleCode.GYM_ADMIN, effective);
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
        if (ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
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
        if (ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
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
                .filter(role -> ActorAccess.isSuperAdmin(actor)
                        || role.getCode() == RoleCode.GYM_ADMIN
                        || role.getCode() == RoleCode.COACH
                        || role.getCode() == RoleCode.MEMBER)
                .map(role -> new RoleView(role.getId(), role.getCode(), role.getName(), role.getStatus().name(),
                        rolePermissions == null ? 0 : menuPermissionCount(actor, role)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionView> listPermissionCatalog(CurrentActor actor) {
        requireRolePermissionAccess(actor, null, "role:permission:read");
        List<Role> roleList = roles.findAll();
        if (permissions == null || rolePermissions == null || (isTenantRoleAdmin(actor) && tenantRolePermissions == null)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        Map<String, Set<RoleCode>> roleCodesByMenu = new HashMap<>();
        for (Role role : roleList) {
            if (!ActorAccess.isSuperAdmin(actor) && role.getCode() == RoleCode.PLATFORM_ADMIN) continue;
            Set<String> assigned = permissionCodesFor(actor, role);
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
        if (roleCode == null || roleCode == RoleCode.PLATFORM_ADMIN || permissionCodes == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        Long tenantId = requireRolePermissionAccess(actor, roleCode, "role:permission:write");
        if (permissions == null || rolePermissions == null || (tenantId != null && tenantRolePermissions == null)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
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
        Set<String> linked = new java.util.HashSet<>();
        if (tenantId == null) {
            rolePermissions.deleteByRoleId(role.getId());
        } else {
            tenantRolePermissions.deleteByTenantIdAndRoleId(tenantId, role.getId());
        }
        for (String menuCode : normalized) {
            if (tenantId == null) {
                linkPermission(role, catalog, linked, menuCode);
            } else {
                linkTenantPermission(tenantId, role, catalog, linked, menuCode);
            }
            for (String operationCode : PermissionCatalog.operationPermissionsFor(roleCode, menuCode)) {
                if (tenantId == null) {
                    linkPermission(role, catalog, linked, operationCode);
                } else {
                    linkTenantPermission(tenantId, role, catalog, linked, operationCode);
                }
            }
        }
        if (tenantId == null) {
            rolePermissions.save(RolePermission.link(role, configuredMarker));
        } else {
            tenantRolePermissions.save(TenantRolePermission.link(tenantReference(tenantId), role, configuredMarker));
        }
    }

    private int menuPermissionCount(CurrentActor actor, Role role) {
        Set<String> assigned = permissionCodesFor(actor, role);
        if (assigned == null) return 0;
        return (int) assigned.stream().filter(PermissionCatalog::isMenuPermission).count();
    }

    private Set<String> permissionCodesFor(CurrentActor actor, Role role) {
        if (isTenantRoleAdmin(actor)
                && (role.getCode() == RoleCode.COACH || role.getCode() == RoleCode.MEMBER)
                && tenantRolePermissions.existsByTenantIdAndRoleId(ActorAccess.requireTenantId(actor), role.getId())) {
            return tenantRolePermissions.findPermissionCodesByTenantIdAndRoleId(
                    ActorAccess.requireTenantId(actor), role.getId());
        }
        return rolePermissions.findPermissionCodesByRoleId(role.getId());
    }

    private void linkTenantPermission(Long tenantId, Role role, Map<String, Permission> catalog,
                                      Set<String> linked, String code) {
        if (!linked.add(code)) return;
        Permission permission = catalog.get(code);
        if (permission == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        tenantRolePermissions.save(TenantRolePermission.link(tenantReference(tenantId), role, permission));
    }

    private Tenant tenantReference(Long tenantId) {
        if (tenants == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        return tenants.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
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

    private static boolean isTenantRoleAdmin(CurrentActor actor) {
        return actor != null && !actor.isPlatformAdmin() && actor.tenantId() != null
                && actor.roles().contains(RoleCode.GYM_ADMIN);
    }

    private Long requireRolePermissionAccess(CurrentActor actor, RoleCode roleCode, String permission) {
        if (actor == null || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (actor.isPlatformAdmin()) {
            return null;
        }
        if (!isTenantRoleAdmin(actor) || (roleCode != null
                && (roleCode == RoleCode.PLATFORM_ADMIN || roleCode == RoleCode.GYM_ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return ActorAccess.requireTenantId(actor);
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

    private static Long commandTenant(CurrentActor actor, Long requestedTenantId) {
        Long tenantId = requestedTenantId != null ? requestedTenantId : ActorAccess.tenantId(actor);
        if (tenantId == null || tenantId <= 0) throw new BusinessException(ErrorCode.TENANT_CONTEXT_REQUIRED);
        return tenantId;
    }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, label + " must not be blank");
        }
        return value.trim();
    }

    private UserSummary summary(UserAccount user) {
        String tenantName = tenants == null || user.getTenantId() == null ? null
                : tenants.findById(user.getTenantId()).map(t -> t.getName()).orElse(null);
        return new UserSummary(user.getId(), user.getTenantId(), tenantName, user.getNormalizedUsername(),
                user.getNormalizedEmail(), user.getDisplayName(), user.getPhone(), user.getStatus(),
                userRoles.findRoleCodesByUserId(user.getId()), null, null);
    }

    private UserView view(UserAccount user, Set<RoleCode> roleCodes) {
        return new UserView(user.getId(), user.getTenantId(), user.getNormalizedUsername(),
                user.getNormalizedEmail(), user.getDisplayName(), user.getPhone(), user.getStatus(), roleCodes);
    }
}
