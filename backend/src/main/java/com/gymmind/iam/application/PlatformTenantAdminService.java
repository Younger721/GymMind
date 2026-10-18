package com.gymmind.iam.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PlatformTenantAdminService {
    private final UserAccountRepository users;
    private final UserRoleRepository roles;
    private final AuditRecorder audit;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public PlatformTenantAdminService(UserAccountRepository users, UserRoleRepository roles, AuditRecorder audit) {
        this.users = Objects.requireNonNull(users);
        this.roles = Objects.requireNonNull(roles);
        this.audit = Objects.requireNonNull(audit);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdministratorView> list(CurrentActor actor, Long tenantId, Pageable pageable) {
        requirePlatform(actor);
        requireTenant(tenantId);
        Page<UserAccount> page = users.findAllByTenantIdAndRoleCode(
                tenantId, RoleCode.GYM_ADMIN, pageable == null ? Pageable.ofSize(20) : pageable);
        return new PageResponse<>(page.map(PlatformTenantAdminService::view).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public void resetPassword(CurrentActor actor, Long tenantId, Long userId, String password) {
        requirePlatform(actor);
        UserAccount user = findAdministrator(tenantId, userId);
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        user.resetPassword(passwords.encode(password));
        users.save(user);
        audit.record(new AuditEvent("PLATFORM_TENANT_ADMIN_PASSWORD_RESET", "USER", userId,
                AuditResult.SUCCESS, "platform-tenant-admin-service", Map.of("source", "platform-console")));
    }

    @Transactional
    public void changeStatus(CurrentActor actor, Long tenantId, Long userId, UserStatus status) {
        requirePlatform(actor);
        if (status == null || (status != UserStatus.ACTIVE && status != UserStatus.DISABLED)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        UserAccount user = findAdministrator(tenantId, userId);
        if (status == UserStatus.ACTIVE) user.activate(); else user.disable();
        users.save(user);
        audit.record(new AuditEvent("PLATFORM_TENANT_ADMIN_STATUS_CHANGED", "USER", userId,
                AuditResult.SUCCESS, "platform-tenant-admin-service", Map.of("source", "platform-console")));
    }

    private UserAccount findAdministrator(Long tenantId, Long userId) {
        requireTenant(tenantId);
        if (userId == null || userId <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        UserAccount user = users.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Set<RoleCode> userRoles = roles.findRoleCodesByUserId(userId);
        if (userRoles == null || !userRoles.equals(Set.of(RoleCode.GYM_ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    private static AdministratorView view(UserAccount user) {
        return new AdministratorView(user.getId(), user.getTenantId(), user.getNormalizedUsername(),
                user.getNormalizedEmail(), user.getDisplayName(), user.getStatus());
    }

    private static void requirePlatform(CurrentActor actor) {
        if (actor == null || !actor.isPlatformAdmin()) throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private static void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public record AdministratorView(Long id, Long tenantId, String username, String email,
                                    String displayName, UserStatus status) {}
}
