package com.gymmind.shared.security;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.support.SecurityTestActors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAccessGuardTest {

    private final TenantAccessGuard guard = new TenantAccessGuard();

    @Test
    void tenantActorCanEnterTenantScopeAndAccessSameTenant() {
        CurrentActor actor = SecurityTestActors.gymAdmin();

        assertThat(guard.requireTenant(actor)).isEqualTo(10L);
        assertThatCode(() -> guard.requireSameTenant(actor, 10L)).doesNotThrowAnyException();
    }

    @Test
    void platformActorCannotEnterTenantBusinessScope() {
        CurrentActor platform = SecurityTestActors.platformAdmin();

        assertThatThrownBy(() -> guard.requireTenant(platform))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void tenantMismatchIsHiddenAsResourceNotFound() {
        CurrentActor actor = SecurityTestActors.gymAdmin();

        assertThatThrownBy(() -> guard.requireSameTenant(actor, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThatThrownBy(() -> guard.requireSameTenant(actor, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void platformActorGetsForbiddenBeforeTenantComparison() {
        assertThatThrownBy(() -> guard.requireSameTenant(SecurityTestActors.platformAdmin(), 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void permissionIsRequiredFailClosed() {
        CurrentActor actor = SecurityTestActors.gymAdmin();

        assertThatCode(() -> guard.requirePermission(actor, "tenant:settings:write"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.requirePermission(actor, "user:write"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> guard.requirePermission(actor, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
