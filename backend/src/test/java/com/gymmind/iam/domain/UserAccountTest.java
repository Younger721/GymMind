package com.gymmind.iam.domain;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {

    private static final String HASH = "$2a$10$hash";

    @Test
    void tenantUserRequiresTenantButPlatformAdminDoesNot() {
        assertThatThrownBy(() -> UserAccount.tenantUser(null, "admin@example.com", HASH, "Admin"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(UserAccount.platformAdmin("root@example.com", HASH, "Root").getTenantId())
                .isNull();
    }

    @Test
    void emailIsTrimmedAndNormalized() {
        UserAccount user = UserAccount.tenantUser(7L, "  Admin@Example.COM ", HASH, " Admin ");

        assertThat(user.getNormalizedEmail()).isEqualTo("admin@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Admin");
        assertThat(user.getPasswordHash()).isEqualTo(HASH);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void disablingAndResettingPasswordIncrementTokenVersion() {
        UserAccount user = UserAccount.tenantUser(7L, "admin@example.com", HASH, "Admin");
        assertThat(user.getTokenVersion()).isZero();

        user.disable();
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.getTokenVersion()).isOne();

        user.resetPassword("$2a$10$newhash");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newhash");
        assertThat(user.getTokenVersion()).isEqualTo(2);
    }

    @Test
    void changingKeyRoleIncrementsTokenVersion() {
        UserAccount user = UserAccount.tenantUser(7L, "admin@example.com", HASH, "Admin");
        user.changeRole(RoleCode.COACH);
        assertThat(user.getRoleCode()).isEqualTo(RoleCode.COACH);
        assertThat(user.getTokenVersion()).isOne();
    }

    @Test
    void tenantUserRejectsNullRoleCodeImmediately() {
        assertThatThrownBy(() -> UserAccount.tenantUser(7L, "admin@example.com", HASH, "Admin", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changingRoleRejectsNullImmediately() {
        UserAccount user = UserAccount.tenantUser(7L, "admin@example.com", HASH, "Admin");

        assertThatThrownBy(() -> user.changeRole(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assigningRoleIncrementsTokenVersion() {
        UserAccount user = UserAccount.tenantUser(7L, "admin@example.com", HASH, "Admin");
        Role role = Role.of(RoleCode.COACH, "Coach");

        UserRole.assign(user, role);

        assertThat(user.getTokenVersion()).isOne();
        assertThat(user.getRoleCode()).isEqualTo(RoleCode.COACH);
    }
}
