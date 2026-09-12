package com.gymmind.shared.security;

import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentActorTest {

    @Test
    void snapshotsRolesAndPermissionsAsImmutableSets() {
        Set<RoleCode> roles = new HashSet<>(Set.of(RoleCode.GYM_ADMIN));
        Set<String> permissions = new HashSet<>(Set.of("tenant:settings:read"));

        CurrentActor actor = tenantActor(roles, permissions);
        roles.add(RoleCode.COACH);
        permissions.add("tenant:settings:write");

        assertThat(actor.roles()).containsExactly(RoleCode.GYM_ADMIN);
        assertThat(actor.permissions()).containsExactly("tenant:settings:read");
        assertThatThrownBy(() -> actor.roles().add(RoleCode.MEMBER))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> actor.permissions().add("user:read"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void exposesPlatformAndPermissionDecisions() {
        CurrentActor actor = new CurrentActor(
                1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of("platform:tenant:read"),
                0L, "platform-jti");

        assertThat(actor.isPlatformAdmin()).isTrue();
        assertThat(actor.hasPermission("platform:tenant:read")).isTrue();
        assertThat(actor.hasPermission("platform:tenant:write")).isFalse();
        assertThat(actor.hasPermission(null)).isFalse();
    }

    @Test
    void rejectsInvalidIdentityAndTokenMetadata() {
        assertThatThrownBy(() -> new CurrentActor(
                null, 10L, Set.of(RoleCode.MEMBER), Set.of(), 0L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrentActor(
                0L, 10L, Set.of(RoleCode.MEMBER), Set.of(), 0L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrentActor(
                1L, 10L, Set.of(RoleCode.MEMBER), Set.of(), -1L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrentActor(
                1L, 10L, Set.of(RoleCode.MEMBER), Set.of(), 0L, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingRolesAndInvalidPlatformTenantCombinations() {
        assertThatThrownBy(() -> new CurrentActor(
                1L, 10L, Set.of(), Set.of(), 0L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrentActor(
                1L, null, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrentActor(
                1L, null, Set.of(RoleCode.PLATFORM_ADMIN, RoleCode.GYM_ADMIN), Set.of(), 0L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrentActor(
                1L, 10L, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "jti"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CurrentActor tenantActor(Set<RoleCode> roles, Set<String> permissions) {
        return new CurrentActor(2L, 10L, roles, permissions, 0L, "tenant-jti");
    }
}
