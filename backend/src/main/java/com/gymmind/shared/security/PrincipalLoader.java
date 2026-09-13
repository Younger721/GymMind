package com.gymmind.shared.security;

import com.gymmind.iam.application.SessionService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.jwt.JwtClaims;
import com.gymmind.shared.security.jwt.TokenType;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class PrincipalLoader {

    private final SessionService sessions;
    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final TenantRepository tenants;

    public PrincipalLoader(
            SessionService sessions,
            UserAccountRepository users,
            UserRoleRepository userRoles,
            TenantRepository tenants) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.users = Objects.requireNonNull(users, "users");
        this.userRoles = Objects.requireNonNull(userRoles, "userRoles");
        this.tenants = Objects.requireNonNull(tenants, "tenants");
    }

    public CurrentActor load(JwtClaims claims) {
        try {
            return loadVerified(claims);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private CurrentActor loadVerified(JwtClaims claims) {
        Objects.requireNonNull(claims, "claims");
        if (claims.type() != TokenType.ACCESS
                || sessions.isAccessRevoked(claims.tenantId(), claims.userId(), claims.tokenId())) {
            throw new IllegalArgumentException("Access token is not active");
        }

        UserAccount user = users.findById(claims.userId())
                .orElseThrow(() -> new IllegalArgumentException("User is unavailable"));
        if (!Objects.equals(user.getId(), claims.userId())
                || user.getStatus() != UserStatus.ACTIVE
                || user.getTokenVersion() != claims.tokenVersion()
                || !Objects.equals(user.getTenantId(), claims.tenantId())) {
            throw new IllegalArgumentException("User state does not match token");
        }

        Set<RoleCode> roles = userRoles.findRoleCodesByUserId(user.getId());
        Set<String> permissions = userRoles.findPermissionCodesByUserId(user.getId());
        if (claims.tenantId() == null) {
            if (!Set.of(RoleCode.PLATFORM_ADMIN).equals(roles)) {
                throw new IllegalArgumentException("Platform authorization is invalid");
            }
        } else {
            Tenant tenant = tenants.findById(claims.tenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Tenant is unavailable"));
            if (!Objects.equals(tenant.getId(), claims.tenantId()) || !tenant.isActive()) {
                throw new IllegalArgumentException("Tenant state does not match token");
            }
        }

        return new CurrentActor(
                user.getId(), user.getTenantId(), roles, permissions,
                user.getTokenVersion(), claims.tokenId());
    }
}
