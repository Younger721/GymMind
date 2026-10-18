package com.gymmind.iam.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.TenantContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformTenantAdminServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final UserRoleRepository roles = mock(UserRoleRepository.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final PlatformTenantAdminService service = new PlatformTenantAdminService(users, roles, audit);

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void platformCanResetTenantAdminPasswordAndAuditDoesNotContainPassword() {
        UserAccount admin = admin(21L, 10L);
        when(users.findByTenantIdAndId(10L, 21L)).thenReturn(Optional.of(admin));
        when(roles.findRoleCodesByUserId(21L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(users.save(admin)).thenReturn(admin);
        long initialTokenVersion = admin.getTokenVersion();

        service.resetPassword(platformAdmin(), 10L, 21L, "new-secure-password");

        assertThat(admin.getTokenVersion()).isEqualTo(initialTokenVersion + 1);
        assertThat(admin.getPasswordHash()).isNotEqualTo("new-secure-password");
        verify(users).save(admin);
        var event = org.mockito.ArgumentCaptor.forClass(com.gymmind.audit.domain.AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().metadata().toString()).doesNotContain("new-secure-password");
    }

    @Test
    void administratorListUsesRoleFilteredPageTotals() {
        UserAccount admin = admin(21L, 10L);
        var pageRequest = PageRequest.of(0, 1);
        when(users.findAllByTenantIdAndRoleCode(10L, RoleCode.GYM_ADMIN, pageRequest))
                .thenReturn(new PageImpl<>(java.util.List.of(admin), pageRequest, 2));
        when(roles.findRoleCodesByUserId(21L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));

        var page = service.list(platformAdmin(), 10L, pageRequest);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.totalPages()).isEqualTo(2);
        verify(users).findAllByTenantIdAndRoleCode(10L, RoleCode.GYM_ADMIN, pageRequest);
        verify(users, never()).findAllByTenantId(10L, pageRequest);
    }

    @Test
    void platformCannotRescueNonAdminOrCrossTenantAccount() {
        UserAccount member = UserAccount.tenantUser(10L, "member@example.com", "hash", "Member", RoleCode.MEMBER);
        ReflectionTestUtils.setField(member, "id", 22L);
        when(users.findByTenantIdAndId(10L, 22L)).thenReturn(Optional.of(member));
        when(roles.findRoleCodesByUserId(22L)).thenReturn(Set.of(RoleCode.MEMBER));

        assertThatThrownBy(() -> service.changeStatus(platformAdmin(), 10L, 22L, UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> service.resetPassword(platformAdmin(), 11L, 22L, "new-secure-password"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(users, never()).save(any());
        verify(audit, never()).record(any());
    }

    @Test
    void platformCannotUseRescueServiceWithoutPlatformRole() {
        var tenantAdmin = com.gymmind.support.SecurityTestActors.tenantActor(
                10L, Set.of(RoleCode.GYM_ADMIN), Set.of("user:write"));
        assertThatThrownBy(() -> service.changeStatus(tenantAdmin, 10L, 21L, UserStatus.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).findByTenantIdAndId(any(), any());
    }

    private static UserAccount admin(Long userId, Long tenantId) {
        UserAccount admin = UserAccount.tenantUser(tenantId, "admin@example.com", "hash", "Admin", RoleCode.GYM_ADMIN);
        ReflectionTestUtils.setField(admin, "id", userId);
        return admin;
    }
}
