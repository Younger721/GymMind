package com.gymmind.iam.application.port;

import com.gymmind.iam.application.model.RefreshSession;

import java.time.Duration;

public interface SessionStore {

    void storeRefresh(RefreshSession session, Duration ttl);

    boolean consumeRefresh(String namespacedTokenId, String tokenHash);

    void revokeAccess(String namespacedTokenId, Duration ttl);

    boolean isAccessRevoked(String namespacedTokenId);

    void deleteRefresh(String namespacedTokenId);

    void revokeAccessAndDeleteRefresh(
            String namespacedAccessTokenId,
            Duration accessTtl,
            String namespacedRefreshTokenId);
}
