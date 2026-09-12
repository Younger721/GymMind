package com.gymmind.iam.application;

import com.gymmind.iam.application.command.LoginCommand;
import com.gymmind.iam.application.command.LogoutCommand;
import com.gymmind.iam.application.command.RefreshCommand;
import com.gymmind.iam.application.command.RegisterTenantCommand;
import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.jwt.JwtClaims;
import com.gymmind.shared.security.jwt.JwtService;
import com.gymmind.shared.security.jwt.TokenPair;
import com.gymmind.shared.security.jwt.TokenType;
import com.gymmind.tenancy.application.TenantProvisioningService;
import com.gymmind.tenancy.application.TenantRegistrationService;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
public class DefaultAuthApplicationService implements AuthApplicationService {

    private static final String LOGIN_SOURCE_TOKEN_ID = "login";
    private static final String REGISTRATION_SOURCE_TOKEN_ID = "registration";

    private final TenantRegistrationService registrationService;
    private final UserAccountRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DefaultAuthApplicationService(
            TenantRegistrationService registrationService,
            UserAccountRepository userRepository,
            UserRoleRepository userRoleRepository,
            TenantRepository tenantRepository,
            JwtService jwtService,
            SessionService sessionService) {
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.userRoleRepository = Objects.requireNonNull(userRoleRepository, "userRoleRepository");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.jwtService = Objects.requireNonNull(jwtService, "jwtService");
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService");
    }

    @Override
    @Transactional
    public AuthResult registerTenant(RegisterTenantCommand command) {
        TenantProvisioningService.ProvisionedTenant provisioned = registrationService.register(command);
        UserAccount administrator = provisioned.administrator();
        Tenant tenant = provisioned.tenant();
        validateActiveUser(administrator, tenant.getId(), administrator.getTokenVersion());
        if (!tenant.isActive() || !Objects.equals(administrator.getTenantId(), tenant.getId())) {
            throw unauthenticated();
        }
        AuthenticatedAccount account = loadAuthorizations(administrator, REGISTRATION_SOURCE_TOKEN_ID);
        return issue(account);
    }

    @Override
    public AuthResult login(LoginCommand command) {
        if (command == null) {
            throw unauthenticated();
        }
        String normalizedEmail;
        try {
            normalizedEmail = UserAccount.normalizeEmail(command.email());
        } catch (IllegalArgumentException exception) {
            throw unauthenticated();
        }
        UserAccount user = userRepository.findByNormalizedEmail(normalizedEmail)
                .orElseThrow(DefaultAuthApplicationService::unauthenticated);
        if (!passwordMatches(command.password(), user.getPasswordHash())) {
            throw unauthenticated();
        }
        return issue(loadActiveAccount(
                user, user.getTenantId(), user.getTokenVersion(), LOGIN_SOURCE_TOKEN_ID));
    }

    @Override
    public AuthResult refresh(RefreshCommand command) {
        if (command == null) {
            throw unauthenticated();
        }
        JwtClaims claims = jwtService.verify(command.refreshToken(), TokenType.REFRESH);
        UserAccount user = userRepository.findById(claims.userId())
                .orElseThrow(DefaultAuthApplicationService::unauthenticated);
        AuthenticatedAccount account = loadActiveAccount(
                user, claims.tenantId(), claims.tokenVersion(), claims.tokenId());
        String refreshTokenId = sessionService.refreshTokenId(
                claims.tenantId(), claims.userId(), claims.tokenId());
        if (!sessionService.consume(refreshTokenId, command.refreshToken())) {
            throw unauthenticated();
        }
        return issue(account);
    }

    @Override
    public void logout(CurrentActor actor, LogoutCommand command) {
        if (actor == null || command == null) {
            throw unauthenticated();
        }
        JwtClaims accessClaims = jwtService.verify(command.accessToken(), TokenType.ACCESS);
        if (!matchesActor(accessClaims, actor)) {
            throw unauthenticated();
        }
        JwtClaims refreshClaims = jwtService.verify(command.refreshToken(), TokenType.REFRESH);
        if (!sameIdentity(accessClaims, refreshClaims)) {
            throw unauthenticated();
        }

        sessionService.logout(
                accessClaims.tenantId(), accessClaims.userId(),
                accessClaims.tokenId(), accessClaims.expiresAt(), refreshClaims.tokenId());
    }

    private AuthenticatedAccount loadActiveAccount(
            UserAccount user, Long expectedTenantId, long expectedTokenVersion, String sourceTokenId) {
        validateActiveUser(user, expectedTenantId, expectedTokenVersion);
        if (expectedTenantId != null) {
            Tenant tenant = tenantRepository.findById(expectedTenantId)
                    .orElseThrow(DefaultAuthApplicationService::unauthenticated);
            if (!tenant.isActive()) {
                throw unauthenticated();
            }
        }

        return loadAuthorizations(user, sourceTokenId);
    }

    private void validateActiveUser(
            UserAccount user, Long expectedTenantId, long expectedTokenVersion) {
        if (user.getId() == null
                || user.getStatus() != UserStatus.ACTIVE
                || !Objects.equals(user.getTenantId(), expectedTenantId)
                || user.getTokenVersion() != expectedTokenVersion) {
            throw unauthenticated();
        }
    }

    private AuthenticatedAccount loadAuthorizations(UserAccount user, String sourceTokenId) {
        Set<RoleCode> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        Set<String> permissions = userRoleRepository.findPermissionCodesByUserId(user.getId());
        if (roles == null || roles.isEmpty() || permissions == null) {
            throw unauthenticated();
        }
        try {
            CurrentActor actor = new CurrentActor(
                    user.getId(), user.getTenantId(), roles, permissions,
                    user.getTokenVersion(), sourceTokenId);
            return new AuthenticatedAccount(user, actor);
        } catch (IllegalArgumentException exception) {
            throw unauthenticated();
        }
    }

    private AuthResult issue(AuthenticatedAccount account) {
        TokenPair tokens = jwtService.issue(account.actor());
        JwtClaims refreshClaims = jwtService.verify(tokens.refreshToken(), TokenType.REFRESH);
        if (!sameIdentity(account.actor(), refreshClaims)) {
            throw new IllegalStateException("Issued refresh token does not match authenticated account");
        }
        sessionService.store(
                sessionService.refreshSession(
                        refreshClaims.tenantId(), refreshClaims.userId(),
                        refreshClaims.tokenId(), refreshClaims.expiresAt()),
                tokens.refreshToken());
        return new AuthResult(
                new AuthResult.AuthenticatedUser(
                        account.user().getId(), account.user().getTenantId(),
                        account.user().getNormalizedEmail(), account.user().getDisplayName(),
                        account.actor().roles(), account.actor().permissions()),
                tokens);
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        if (rawPassword == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean matchesActor(JwtClaims claims, CurrentActor actor) {
        return Objects.equals(claims.userId(), actor.userId())
                && Objects.equals(claims.tenantId(), actor.tenantId())
                && claims.roles().equals(actor.roles())
                && claims.permissions().equals(actor.permissions())
                && claims.tokenVersion() == actor.tokenVersion()
                && claims.tokenId().equals(actor.tokenId());
    }

    private static boolean sameIdentity(JwtClaims first, JwtClaims second) {
        return Objects.equals(first.userId(), second.userId())
                && Objects.equals(first.tenantId(), second.tenantId())
                && first.roles().equals(second.roles())
                && first.permissions().equals(second.permissions())
                && first.tokenVersion() == second.tokenVersion();
    }

    private static boolean sameIdentity(CurrentActor actor, JwtClaims claims) {
        return Objects.equals(actor.userId(), claims.userId())
                && Objects.equals(actor.tenantId(), claims.tenantId())
                && actor.roles().equals(claims.roles())
                && actor.permissions().equals(claims.permissions())
                && actor.tokenVersion() == claims.tokenVersion();
    }

    private static BusinessException unauthenticated() {
        return new BusinessException(ErrorCode.UNAUTHENTICATED);
    }

    private record AuthenticatedAccount(UserAccount user, CurrentActor actor) {
    }
}
