package com.gymmind.iam.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserStatisticsServiceTest {

    @Test
    void platformAdminListsAllUsers() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        UserRoleRepository roles = mock(UserRoleRepository.class);
        UserAccount platform = user(1L, null, RoleCode.PLATFORM_ADMIN);
        UserAccount member = user(2L, 10L, RoleCode.MEMBER);
        when(users.findAll()).thenReturn(List.of(platform, member));
        when(roles.findRoleCodesByUserId(1L)).thenReturn(Set.of(RoleCode.PLATFORM_ADMIN));
        when(roles.findRoleCodesByUserId(2L)).thenReturn(Set.of(RoleCode.MEMBER));

        UserStatisticsService service = new DefaultUserStatisticsService(users, roles);

        assertThat(service.list(actor(null, RoleCode.PLATFORM_ADMIN), PageRequest.of(0, 20)).content())
                .extracting(UserAdministrationService.UserSummary::id)
                .containsExactly(1L, 2L);
    }

    @Test
    void gymAdminListsOnlyOwnAccountCoachesAndMembers() {
        UserAccount admin = user(1L, 10L, RoleCode.GYM_ADMIN);
        UserAccount coach = user(2L, 10L, RoleCode.COACH);
        UserAccount member = user(3L, 10L, RoleCode.MEMBER);
        UserAccount otherAdmin = user(4L, 10L, RoleCode.GYM_ADMIN);
        UserAccount otherTenant = user(5L, 11L, RoleCode.MEMBER);
        UserAccountRepository users = mock(UserAccountRepository.class);
        UserRoleRepository roles = mock(UserRoleRepository.class);
        when(users.findAllByTenantId(10L, PageRequest.of(0, 1000)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(admin, coach, member, otherAdmin), PageRequest.of(0, 1000), 4));
        when(roles.findRoleCodesByUserId(1L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(roles.findRoleCodesByUserId(2L)).thenReturn(Set.of(RoleCode.COACH));
        when(roles.findRoleCodesByUserId(3L)).thenReturn(Set.of(RoleCode.MEMBER));
        when(roles.findRoleCodesByUserId(4L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));

        UserStatisticsService service = new DefaultUserStatisticsService(users, roles);

        assertThat(service.list(actor(10L, RoleCode.GYM_ADMIN), PageRequest.of(0, 20)).content())
                .extracting(UserAdministrationService.UserSummary::id)
                .containsExactly(1L, 2L, 3L);
        assertThat(otherTenant.getTenantId()).isEqualTo(11L);
    }

    private static UserAccount user(Long id, Long tenantId, RoleCode role) {
        UserAccount value = tenantId == null
                ? UserAccount.platformAdmin("user" + id + "@example.com", "user" + id, "hash", "User " + id)
                : UserAccount.tenantUser(tenantId, "user" + id + "@example.com", "user" + id, "hash", "User " + id, role);
        org.springframework.test.util.ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private static CurrentActor actor(Long tenantId, RoleCode role) {
        return new CurrentActor(1L, tenantId, Set.of(role), Set.of(), 0L, "token-" + role);
    }
}
