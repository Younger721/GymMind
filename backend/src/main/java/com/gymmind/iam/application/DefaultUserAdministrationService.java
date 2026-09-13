package com.gymmind.iam.application;

import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class DefaultUserAdministrationService implements UserAdministrationService {

    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DefaultUserAdministrationService(UserAccountRepository users,
                                             UserRoleRepository userRoles,
                                             RoleRepository roles) {
        this.users = Objects.requireNonNull(users, "users");
        this.userRoles = Objects.requireNonNull(userRoles, "userRoles");
        this.roles = Objects.requireNonNull(roles, "roles");
    }

    @Override
    @Transactional
    public UserView create(CurrentActor actor, CreateUserCommand command) {
        requireGymAdmin(actor, "user:write");
        Objects.requireNonNull(command, "command");
        RoleCode roleCode = requireTenantRole(command.role());
        String email = UserAccount.normalizeEmail(command.email());
        if (users.findByNormalizedEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        UserAccount user = UserAccount.tenantUser(actor.tenantId(), email,
                passwordEncoder.encode(required(command.password(), "Password")),
                required(command.displayName(), "Display name"), roleCode);
        UserAccount saved = users.save(user);
        Role role = roles.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        userRoles.save(UserRole.assign(saved, role));
        return view(saved, Set.of(roleCode));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummary> list(CurrentActor actor, Pageable pageable) {
        requireGymAdmin(actor, "user:read");
        Pageable effective = pageable == null ? Pageable.ofSize(20) : pageable;
        Page<UserAccount> page = users.findAllByTenantId(actor.tenantId(), effective);
        return new PageResponse<>(page.map(this::summary).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public void changeStatus(CurrentActor actor, Long userId, UserStatus status) {
        requireGymAdmin(actor, "user:write");
        if (status == null || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        UserAccount user = findTenantUser(actor, userId);
        requireManagedUser(userId);
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
        requireGymAdmin(actor, "role:assign");
        if (userId == null || userId <= 0 || roleCodes == null || roleCodes.isEmpty()
                || roleCodes.stream().anyMatch(code -> code == null || code == RoleCode.PLATFORM_ADMIN
                || code == RoleCode.GYM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        UserAccount user = findTenantUser(actor, userId);
        requireManagedUser(userId);
        Set<RoleCode> current = userRoles.findRoleCodesByUserId(userId);
        if (!Objects.equals(current, roleCodes)) {
            RoleCode primary = roleCodes.stream().sorted(Comparator.comparing(Enum::name)).findFirst().orElseThrow();
            user.changeRole(primary);
            userRoles.deleteByUserId(userId);
            for (RoleCode code : roleCodes) {
                Role role = roles.findByCode(code)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
                userRoles.save(UserRole.assign(user, role));
            }
            users.save(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleView> listRoles(CurrentActor actor) {
        requireGymAdmin(actor, "role:read");
        return roles.findAll().stream()
                .filter(role -> role.getCode() != RoleCode.PLATFORM_ADMIN && role.getCode() != RoleCode.GYM_ADMIN)
                .map(role -> new RoleView(role.getId(), role.getCode(), role.getName()))
                .toList();
    }

    private UserAccount findTenantUser(CurrentActor actor, Long userId) {
        return users.findByTenantIdAndId(actor.tenantId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void requireManagedUser(Long userId) {
        Set<RoleCode> currentRoles = userRoles.findRoleCodesByUserId(userId);
        if (currentRoles != null && currentRoles.stream()
                .anyMatch(code -> code == RoleCode.PLATFORM_ADMIN || code == RoleCode.GYM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireGymAdmin(CurrentActor actor, String permission) {
        if (actor == null || actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN)
                || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static RoleCode requireTenantRole(RoleCode role) {
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
        return new UserSummary(user.getId(), user.getTenantId(), user.getNormalizedEmail(),
                user.getDisplayName(), user.getStatus(), userRoles.findRoleCodesByUserId(user.getId()));
    }

    private UserView view(UserAccount user, Set<RoleCode> roleCodes) {
        return new UserView(user.getId(), user.getTenantId(), user.getNormalizedEmail(),
                user.getDisplayName(), user.getStatus(), roleCodes);
    }
}
