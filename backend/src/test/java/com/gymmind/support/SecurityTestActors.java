package com.gymmind.support;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;

import java.util.Set;

public final class SecurityTestActors {

    private SecurityTestActors() {
    }

    public static CurrentActor platformAdmin() {
        return new CurrentActor(
                1L,
                null,
                Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:read", "platform:tenant:write"),
                0L,
                "platform-token-id");
    }

    public static CurrentActor gymAdmin() {
        return tenantActor(10L, Set.of(RoleCode.GYM_ADMIN),
                Set.of("tenant:settings:read", "tenant:settings:write"));
    }

    public static CurrentActor tenantActor(Long tenantId, Set<RoleCode> roles, Set<String> permissions) {
        return new CurrentActor(2L, tenantId, roles, permissions, 3L, "tenant-token-id");
    }
}
