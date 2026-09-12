package com.gymmind.iam.application;

import com.gymmind.iam.application.command.LoginCommand;
import com.gymmind.iam.application.command.LogoutCommand;
import com.gymmind.iam.application.command.RefreshCommand;
import com.gymmind.iam.application.command.RegisterTenantCommand;
import com.gymmind.iam.application.model.RefreshSession;
import com.gymmind.iam.application.port.SessionStore;
import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-12T09:00:00Z");
    private static final RegisterTenantCommand REGISTER_COMMAND = new RegisterTenantCommand(
            "new-gym", "New Gym", " Owner@Example.COM ", "correct-password", "Owner");

    private UserAccountRepository users;
    private UserRoleRepository userRoles;
    private TenantRepository tenants;
    private TenantRegistrationService registrations;
    private RecordingJwtService jwt;
    private RecordingSessionStore sessionStore;
    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        users = mock(UserAccountRepository.class);
        userRoles = mock(UserRoleRepository.class);
        tenants = mock(TenantRepository.class);
        registrations = mock(TenantRegistrationService.class);
        jwt = new RecordingJwtService();
        sessionStore = new RecordingSessionStore();
        SessionService sessions = new SessionService(
                sessionStore, Clock.fixed(NOW, ZoneOffset.UTC), "test");
        service = new DefaultAuthApplicationService(
                registrations, users, userRoles, tenants, jwt, sessions);
    }

    @Test
    void publicRegistrationAlwaysCreatesGymAdmin() {
        Tenant tenant = tenant(7L, "new-gym", "New Gym");
        UserAccount user = user(42L, tenant.getId(), RoleCode.MEMBER);
        when(registrations.register(REGISTER_COMMAND))
                .thenReturn(new TenantProvisioningService.ProvisionedTenant(tenant, user));
        when(userRoles.findRoleCodesByUserId(42L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(userRoles.findPermissionCodesByUserId(42L)).thenReturn(Set.of("tenant:settings:write"));

        AuthResult result = service.registerTenant(REGISTER_COMMAND);

        assertThat(result.user().roles()).containsExactly(RoleCode.GYM_ADMIN);
        assertThat(jwt.lastIssued.roles()).containsExactly(RoleCode.GYM_ADMIN);
        assertThat(result.tokens().refreshToken()).isNotBlank();
    }

    @Test
    void loginNormalizesEmailAndUsesRepositoryRoleAssignments() {
        Tenant tenant = tenant(7L, "new-gym", "New Gym");
        UserAccount user = user(42L, tenant.getId(), RoleCode.MEMBER);
        when(users.findByNormalizedEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(tenants.findById(7L)).thenReturn(Optional.of(tenant));
        when(userRoles.findRoleCodesByUserId(42L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(userRoles.findPermissionCodesByUserId(42L)).thenReturn(Set.of("tenant:settings:write"));

        AuthResult result = service.login(new LoginCommand(" Owner@Example.COM ", "correct-password"));

        assertThat(result.user().email()).isEqualTo("owner@example.com");
        assertThat(result.user().roles()).containsExactly(RoleCode.GYM_ADMIN);
        assertThat(result.user().permissions()).containsExactly("tenant:settings:write");
    }

    @Test
    void loginFailuresDoNotRevealEmailPasswordUserOrTenant() {
        assertUnauthenticated(() -> service.login(new LoginCommand("missing@example.com", "wrong")));

        UserAccount user = user(42L, 7L, RoleCode.GYM_ADMIN);
        when(users.findByNormalizedEmail("owner@example.com")).thenReturn(Optional.of(user));
        assertUnauthenticated(() -> service.login(new LoginCommand("owner@example.com", "wrong")));

        user.disable();
        assertUnauthenticated(() -> service.login(new LoginCommand("owner@example.com", "correct-password")));

        UserAccount activeUser = user(43L, 8L, RoleCode.GYM_ADMIN);
        when(users.findByNormalizedEmail("other@example.com")).thenReturn(Optional.of(activeUser));
        Tenant disabledTenant = tenant(8L, "disabled", "Disabled");
        disabledTenant.disable();
        when(tenants.findById(8L)).thenReturn(Optional.of(disabledTenant));
        assertUnauthenticated(() -> service.login(new LoginCommand("other@example.com", "correct-password")));
    }

    @Test
    void refreshRevalidatesStateThenConsumesAndRotatesExactlyOnce() {
        Tenant tenant = tenant(7L, "new-gym", "New Gym");
        UserAccount user = user(42L, 7L, RoleCode.MEMBER);
        stubActiveAccount(user, tenant);
        AuthResult login = service.login(new LoginCommand("owner@example.com", "correct-password"));
        String originalRefresh = login.tokens().refreshToken();

        AuthResult refreshed = service.refresh(new RefreshCommand(originalRefresh));

        assertThat(refreshed.tokens().refreshToken()).isNotEqualTo(originalRefresh);
        assertThat(sessionStore.refreshSessions).hasSize(1);
        assertUnauthenticated(() -> service.refresh(new RefreshCommand(originalRefresh)));
        assertThat(jwt.sequence).isEqualTo(2);
    }

    @Test
    void disabledUserOrTenantAndTokenVersionMismatchCannotRefresh() {
        Tenant tenant = tenant(7L, "new-gym", "New Gym");
        UserAccount user = user(42L, 7L, RoleCode.GYM_ADMIN);
        stubActiveAccount(user, tenant);
        String disabledUserToken = service.login(
                new LoginCommand("owner@example.com", "correct-password")).tokens().refreshToken();
        user.disable();
        int consumeAttempts = sessionStore.consumeAttempts;
        assertUnauthenticated(() -> service.refresh(new RefreshCommand(disabledUserToken)));
        assertThat(sessionStore.consumeAttempts).isEqualTo(consumeAttempts);

        UserAccount second = user(43L, 8L, RoleCode.GYM_ADMIN);
        Tenant secondTenant = tenant(8L, "second", "Second");
        when(users.findByNormalizedEmail("second@example.com")).thenReturn(Optional.of(second));
        when(users.findById(43L)).thenReturn(Optional.of(second));
        when(tenants.findById(8L)).thenReturn(Optional.of(secondTenant));
        when(userRoles.findRoleCodesByUserId(43L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(userRoles.findPermissionCodesByUserId(43L)).thenReturn(Set.of());
        String disabledTenantToken = service.login(
                new LoginCommand("second@example.com", "correct-password")).tokens().refreshToken();
        secondTenant.disable();
        consumeAttempts = sessionStore.consumeAttempts;
        assertUnauthenticated(() -> service.refresh(new RefreshCommand(disabledTenantToken)));
        assertThat(sessionStore.consumeAttempts).isEqualTo(consumeAttempts);

        UserAccount third = user(44L, 9L, RoleCode.GYM_ADMIN);
        Tenant thirdTenant = tenant(9L, "third", "Third");
        when(users.findByNormalizedEmail("third@example.com")).thenReturn(Optional.of(third));
        when(users.findById(44L)).thenReturn(Optional.of(third));
        when(tenants.findById(9L)).thenReturn(Optional.of(thirdTenant));
        when(userRoles.findRoleCodesByUserId(44L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(userRoles.findPermissionCodesByUserId(44L)).thenReturn(Set.of());
        String staleToken = service.login(
                new LoginCommand("third@example.com", "correct-password")).tokens().refreshToken();
        third.incrementTokenVersion();
        consumeAttempts = sessionStore.consumeAttempts;
        assertUnauthenticated(() -> service.refresh(new RefreshCommand(staleToken)));
        assertThat(sessionStore.consumeAttempts).isEqualTo(consumeAttempts);
    }

    @Test
    void logoutUsesVerifiedAccessExpiryAndRemovesMatchingRefreshSession() {
        Tenant tenant = tenant(7L, "new-gym", "New Gym");
        UserAccount user = user(42L, 7L, RoleCode.GYM_ADMIN);
        stubActiveAccount(user, tenant);
        AuthResult login = service.login(new LoginCommand("owner@example.com", "correct-password"));
        JwtClaims access = jwt.verify(login.tokens().accessToken(), TokenType.ACCESS);

        service.logout(access.toCurrentActor(),
                new LogoutCommand(login.tokens().accessToken(), login.tokens().refreshToken()));

        assertThat(sessionStore.revokedTokenId).endsWith(":access:" + access.tokenId());
        assertThat(sessionStore.revokeTtl).isEqualTo(Duration.ofMinutes(15));
        assertThat(sessionStore.refreshSessions).isEmpty();
    }

    @Test
    void logoutRejectsAnAccessTokenThatDoesNotMatchTheAuthenticatedActor() {
        CurrentActor attacker = new CurrentActor(
                99L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "attacker-jti");
        TokenPair victimTokens = jwt.issue(new CurrentActor(
                42L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "source"));

        assertUnauthenticated(() -> service.logout(attacker,
                new LogoutCommand(victimTokens.accessToken(), victimTokens.refreshToken())));
        assertThat(sessionStore.revokedTokenId).isNull();
    }

    @Test
    void logoutValidatesRefreshTokenBeforeRevokingAccess() {
        Tenant tenant = tenant(7L, "new-gym", "New Gym");
        UserAccount user = user(42L, 7L, RoleCode.GYM_ADMIN);
        stubActiveAccount(user, tenant);
        AuthResult login = service.login(new LoginCommand("owner@example.com", "correct-password"));
        JwtClaims access = jwt.verify(login.tokens().accessToken(), TokenType.ACCESS);

        assertUnauthenticated(() -> service.logout(access.toCurrentActor(),
                new LogoutCommand(login.tokens().accessToken(), "invalid-refresh")));

        assertThat(sessionStore.revokedTokenId).isNull();
        assertThat(sessionStore.refreshSessions).hasSize(1);
    }

    private void stubActiveAccount(UserAccount user, Tenant tenant) {
        when(users.findByNormalizedEmail(user.getNormalizedEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(tenants.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(userRoles.findRoleCodesByUserId(user.getId())).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(userRoles.findPermissionCodesByUserId(user.getId())).thenReturn(Set.of());
    }

    private static UserAccount user(Long id, Long tenantId, RoleCode transientRole) {
        String email = switch (id.intValue()) {
            case 43 -> "second@example.com";
            case 44 -> "third@example.com";
            default -> "owner@example.com";
        };
        UserAccount user = UserAccount.tenantUser(tenantId, email,
                new BCryptPasswordEncoder().encode("correct-password"), "Owner", transientRole);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Tenant tenant(Long id, String code, String name) {
        Tenant tenant = Tenant.create(code, name);
        ReflectionTestUtils.setField(tenant, "id", id);
        return tenant;
    }

    private static void assertUnauthenticated(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
                    assertThat(exception.getMessage()).isEqualTo("Authentication required");
                    assertThat(exception.getCause()).isNull();
                });
    }

    private static final class RecordingJwtService implements JwtService {
        private final Map<String, JwtClaims> claims = new HashMap<>();
        private int sequence;
        private CurrentActor lastIssued;

        @Override
        public TokenPair issue(CurrentActor actor) {
            lastIssued = actor;
            sequence++;
            String access = "access-token-" + sequence;
            String refresh = "refresh-token-" + sequence;
            Instant accessExpiry = NOW.plus(Duration.ofMinutes(15));
            Instant refreshExpiry = NOW.plus(Duration.ofDays(7));
            claims.put(access, claims(actor, "access-jti-" + sequence, TokenType.ACCESS, accessExpiry));
            claims.put(refresh, claims(actor, "refresh-jti-" + sequence, TokenType.REFRESH, refreshExpiry));
            return new TokenPair(access, accessExpiry, refresh, refreshExpiry);
        }

        @Override
        public JwtClaims verify(String rawToken, TokenType expectedType) {
            JwtClaims value = claims.get(rawToken);
            if (value == null || value.type() != expectedType) {
                throw new BusinessException(ErrorCode.UNAUTHENTICATED);
            }
            return value;
        }

        private JwtClaims claims(CurrentActor actor, String tokenId, TokenType type, Instant expiresAt) {
            return new JwtClaims("GymMind", actor.userId(), actor.tenantId(), actor.roles(), actor.permissions(),
                    actor.tokenVersion(), tokenId, type, NOW, expiresAt);
        }
    }

    private static final class RecordingSessionStore implements SessionStore {
        private final Map<String, RefreshSession> refreshSessions = new HashMap<>();
        private String revokedTokenId;
        private Duration revokeTtl;
        private int consumeAttempts;

        @Override
        public void storeRefresh(RefreshSession session, Duration ttl) {
            refreshSessions.put(session.tokenId(), session);
        }

        @Override
        public boolean consumeRefresh(String namespacedTokenId, String tokenHash) {
            consumeAttempts++;
            RefreshSession stored = refreshSessions.get(namespacedTokenId);
            if (stored == null || !stored.tokenHash().equals(tokenHash)) return false;
            refreshSessions.remove(namespacedTokenId);
            return true;
        }

        @Override
        public void revokeAccess(String namespacedTokenId, Duration ttl) {
            revokedTokenId = namespacedTokenId;
            revokeTtl = ttl;
        }

        @Override
        public boolean isAccessRevoked(String namespacedTokenId) {
            return namespacedTokenId.equals(revokedTokenId);
        }

        @Override
        public void deleteRefresh(String namespacedTokenId) {
            refreshSessions.remove(namespacedTokenId);
        }
    }
}
