package com.gymmind.iam.application;

import com.gymmind.iam.application.model.RefreshSession;
import com.gymmind.iam.application.port.SessionStore;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-12T08:00:00Z");
    private static final String RAW_REFRESH = "refresh-token-secret";
    private static final String EXPECTED_HASH =
            "557888b8721a0f0bff6ef70bcfd0032d0aa38fd0e9057b40afee31925d1fc762";

    private RecordingSessionStore store;
    private SessionService service;
    private RefreshSession session;

    @BeforeEach
    void setUp() {
        store = new RecordingSessionStore();
        service = new SessionService(store, Clock.fixed(NOW, ZoneOffset.UTC), "test");
        session = service.refreshSession(7L, 42L, "refresh-jti", NOW.plusSeconds(90));
    }

    @Test
    void storesOnlySha256DigestWithRemainingTtl() {
        service.store(session, RAW_REFRESH);

        RefreshSession stored = store.refreshSessions.get(session.tokenId());
        assertThat(stored.tokenHash()).isEqualTo(EXPECTED_HASH).doesNotContain(RAW_REFRESH);
        assertThat(store.refreshTtl).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    void refreshTokenCanBeConsumedOnlyOnce() {
        service.store(session, RAW_REFRESH);

        assertThat(service.consume(session.tokenId(), RAW_REFRESH)).isTrue();
        assertThat(service.consume(session.tokenId(), RAW_REFRESH)).isFalse();
    }

    @Test
    void wrongRefreshTokenDoesNotConsumeStoredSession() {
        service.store(session, RAW_REFRESH);

        assertThat(service.consume(session.tokenId(), "wrong-token")).isFalse();
        assertThat(service.consume(session.tokenId(), RAW_REFRESH)).isTrue();
    }

    @Test
    void buildsExactTenantAndPlatformNamespaces() {
        assertThat(service.refreshTokenId(7L, 42L, "refresh-jti"))
                .isEqualTo("gymmind:test:v1:auth:7:42:refresh:refresh-jti");
        assertThat(service.accessTokenId(null, 1L, "access-jti"))
                .isEqualTo("gymmind:test:v1:auth:platform:1:access:access-jti");
    }

    @Test
    void revokesAndChecksAccessTokenUsingItsExpiry() {
        service.revokeAccess(7L, 42L, "access-jti", NOW.plusSeconds(30));

        assertThat(store.revokedTokenId)
                .isEqualTo("gymmind:test:v1:auth:7:42:access:access-jti");
        assertThat(store.revokeTtl).isEqualTo(Duration.ofSeconds(30));
        assertThat(service.isAccessRevoked(7L, 42L, "access-jti")).isTrue();
    }

    @Test
    void atomicallyRevokesAccessAndDeletesRefreshUsingAccessExpiry() {
        service.store(session, RAW_REFRESH);

        service.logout(7L, 42L, "access-jti", NOW.plusSeconds(30), "refresh-jti");

        assertThat(store.revokedTokenId)
                .isEqualTo("gymmind:test:v1:auth:7:42:access:access-jti");
        assertThat(store.revokeTtl).isEqualTo(Duration.ofSeconds(30));
        assertThat(store.refreshSessions).isEmpty();
        assertThat(store.atomicLogoutCalls).isEqualTo(1);
    }

    @Test
    void atomicLogoutFailureLeavesNoPartialState() {
        service.store(session, RAW_REFRESH);
        store.failure = new IllegalStateException("redis unavailable");

        assertThatThrownBy(() -> service.logout(
                7L, 42L, "access-jti", NOW.plusSeconds(30), "refresh-jti"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE));

        assertThat(store.revokedTokenId).isNull();
        assertThat(store.refreshSessions).containsKey(session.tokenId());
    }

    @Test
    void rejectsExpiredSessionsAndInvalidKeyParts() {
        RefreshSession expired = service.refreshSession(7L, 42L, "expired", NOW);

        assertThatThrownBy(() -> service.store(expired, RAW_REFRESH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.refreshTokenId(0L, 42L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.refreshTokenId(7L, 0L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.refreshTokenId(7L, 42L, "bad:jti"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redisFailuresAreNeverReportedAsAValidSessionState() {
        store.failure = new IllegalStateException("redis unavailable with internal details");

        assertThatThrownBy(() -> service.isAccessRevoked(7L, 42L, "access-jti"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE);
                    assertThat(exception.getMessage()).isEqualTo("Dependency unavailable");
                    assertThat(exception.getCause()).isNull();
                });
        assertThatThrownBy(() -> service.consume(session.tokenId(), RAW_REFRESH))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE));
    }

    private static final class RecordingSessionStore implements SessionStore {
        private final Map<String, RefreshSession> refreshSessions = new HashMap<>();
        private Duration refreshTtl;
        private String revokedTokenId;
        private Duration revokeTtl;
        private int atomicLogoutCalls;
        private RuntimeException failure;

        @Override
        public void storeRefresh(RefreshSession session, Duration ttl) {
            failIfConfigured();
            refreshSessions.put(session.tokenId(), session);
            refreshTtl = ttl;
        }

        @Override
        public boolean consumeRefresh(String namespacedTokenId, String tokenHash) {
            failIfConfigured();
            RefreshSession existing = refreshSessions.get(namespacedTokenId);
            if (existing != null && existing.tokenHash().equals(tokenHash)) {
                refreshSessions.remove(namespacedTokenId);
                return true;
            }
            return false;
        }

        @Override
        public void revokeAccess(String namespacedTokenId, Duration ttl) {
            failIfConfigured();
            revokedTokenId = namespacedTokenId;
            revokeTtl = ttl;
        }

        @Override
        public boolean isAccessRevoked(String namespacedTokenId) {
            failIfConfigured();
            return namespacedTokenId.equals(revokedTokenId);
        }

        @Override
        public void deleteRefresh(String namespacedTokenId) {
            failIfConfigured();
            refreshSessions.remove(namespacedTokenId);
        }

        @Override
        public void revokeAccessAndDeleteRefresh(
                String namespacedAccessTokenId,
                Duration accessTtl,
                String namespacedRefreshTokenId) {
            failIfConfigured();
            revokedTokenId = namespacedAccessTokenId;
            revokeTtl = accessTtl;
            refreshSessions.remove(namespacedRefreshTokenId);
            atomicLogoutCalls++;
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}
