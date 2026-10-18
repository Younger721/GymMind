package com.gymmind.shared.security;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActorAccessTest {

    @Test
    void requireGymAdminRejectsMissingPermission() {
        CurrentActor admin = new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "t");
        assertThatThrownBy(() -> ActorAccess.requireGymAdmin(admin, "user:write"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void requireGymAdminAllowsWhenPermissionPresent() {
        CurrentActor admin = new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("user:write"), 0L, "t");
        assertThatCode(() -> ActorAccess.requireGymAdmin(admin, "user:write"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireExplicitGymAdminAlsoChecksPermission() {
        CurrentActor admin = new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("coach:assign"), 0L, "t");
        assertThatThrownBy(() -> ActorAccess.requireExplicitGymAdmin(admin, "user:write"))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> ActorAccess.requireExplicitGymAdmin(admin, "coach:assign"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireExplicitGymAdminRejectsPlatformAdmin() {
        CurrentActor platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "t");
        TenantContextHolder.set(7L);
        try {
            assertThatThrownBy(() -> ActorAccess.requireExplicitGymAdmin(platform, "coach:assign"))
                    .isInstanceOf(BusinessException.class);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void requireGymAdminApprovalRejectsPlatformAdmin() {
        CurrentActor platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("membership:change:approve"), 0L, "t");
        assertThatThrownBy(() -> ActorAccess.requireGymAdminApproval(platform, "membership:change:approve"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void requireGymAdminAllowsPlatformReadButRejectsTenantBusinessWrite() {
        CurrentActor platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "t");
        TenantContextHolder.set(7L);
        assertThatCode(() -> ActorAccess.requireGymAdmin(platform, "member:read")).doesNotThrowAnyException();
        assertThatThrownBy(() -> ActorAccess.requireGymAdmin(platform, "member:write"))
                .isInstanceOf(BusinessException.class);
        TenantContextHolder.clear();
    }
}
