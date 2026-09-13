package com.gymmind.iam.application;

import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdministrationServiceTest {

    private UserAccountRepository users;
    private UserRoleRepository userRoles;
    private RoleRepository roles;
    private UserAdministrationService service;

    private static final CurrentActor ADMIN = new CurrentActor(
            2L, 10L, Set.of(RoleCode.GYM_ADMIN),
            Set.of("user:read", "user:write", "role:read", "role:assign"), 3L, "admin-token");

    @BeforeEach
    void setUp() {
        users = mock(UserAccountRepository.class);
        userRoles = mock(UserRoleRepository.class);
        roles = mock(RoleRepository.class);
        service = new DefaultUserAdministrationService(users, userRoles, roles);
    }

    @Test
    void gymAdminCreatesOnlyTenantCoachOrMemberWithHashedPassword() {
        UserAccount saved = UserAccount.tenantUser(10L, "coach@example.com", "$2a$10$hash", "Coach", RoleCode.COACH);
        ReflectionTestUtils.setField(saved, "id", 20L);
        Role coach = role(2L, RoleCode.COACH);
        when(users.save(any())).thenReturn(saved);
        when(roles.findByCode(RoleCode.COACH)).thenReturn(Optional.of(coach));

        UserAdministrationService.UserView result = service.create(ADMIN,
                new CreateUserCommand(" Coach@Example.COM ", "password123", "Coach", RoleCode.COACH));

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.tenantId()).isEqualTo(10L);
        verify(users).save(any(UserAccount.class));
        verify(userRoles).save(any());
    }

    @Test
    void gymAdminCannotGrantPlatformOrGymAdminRole() {
        assertThatThrownBy(() -> service.replaceRoles(ADMIN, 20L, Set.of(RoleCode.PLATFORM_ADMIN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> service.replaceRoles(ADMIN, 20L, Set.of(RoleCode.GYM_ADMIN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(users, never()).findByTenantIdAndId(any(), any());
    }

    @Test
    void crossTenantUserIsHiddenAsNotFound() {
        when(users.findByTenantIdAndId(10L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(ADMIN, 99L, UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void statusAndRoleChangesIncrementTokenVersion() {
        UserAccount user = UserAccount.tenantUser(10L, "member@example.com", "hash", "Member", RoleCode.MEMBER);
        ReflectionTestUtils.setField(user, "id", 20L);
        Role coach = role(2L, RoleCode.COACH);
        when(users.findByTenantIdAndId(10L, 20L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        when(roles.findByCode(RoleCode.COACH)).thenReturn(Optional.of(coach));

        service.changeStatus(ADMIN, 20L, UserStatus.DISABLED);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        service.replaceRoles(ADMIN, 20L, Set.of(RoleCode.COACH));
        assertThat(user.getTokenVersion()).isEqualTo(2L);
    }

    @Test
    void gymAdminCannotManageAnotherGymAdmin() {
        UserAccount otherAdmin = UserAccount.tenantUser(10L, "other-admin@example.com", "hash", "Admin", RoleCode.GYM_ADMIN);
        ReflectionTestUtils.setField(otherAdmin, "id", 21L);
        when(users.findByTenantIdAndId(10L, 21L)).thenReturn(Optional.of(otherAdmin));
        when(userRoles.findRoleCodesByUserId(21L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));

        assertThatThrownBy(() -> service.changeStatus(ADMIN, 21L, UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void listIsTenantScopedAndReturnsPageEnvelope() {
        UserAccount user = UserAccount.tenantUser(10L, "member@example.com", "hash", "Member", RoleCode.MEMBER);
        ReflectionTestUtils.setField(user, "id", 20L);
        when(users.findAllByTenantId(10L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        PageResponse<UserAdministrationService.UserSummary> page = service.list(ADMIN, PageRequest.of(0, 20));

        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.content()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(20L);
            assertThat(summary.tenantId()).isEqualTo(10L);
        });
        verify(users).findAllByTenantId(10L, PageRequest.of(0, 20));
    }

    private static Role role(Long id, RoleCode code) {
        Role role = Role.of(code, code.name());
        ReflectionTestUtils.setField(role, "id", id);
        return role;
    }
}
