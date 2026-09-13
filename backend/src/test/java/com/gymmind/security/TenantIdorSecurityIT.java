package com.gymmind.security;

import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TenantIdorSecurityIT extends MySqlIntegrationTest {

    @Autowired
    private TenantRepository tenants;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private UserAdministrationService userAdministration;

    @Test
    void tenantAdminCannotReadOrMutateAnotherTenantsUser() {
        Tenant firstTenant = tenants.save(Tenant.create("idor-first", "IDOR First"));
        Tenant secondTenant = tenants.save(Tenant.create("idor-second", "IDOR Second"));
        UserAccount firstUser = users.save(UserAccount.tenantUser(
                firstTenant.getId(), "first-member@example.com", "hash", "First Member", RoleCode.MEMBER));
        UserAccount secondUser = users.save(UserAccount.tenantUser(
                secondTenant.getId(), "second-member@example.com", "hash", "Second Member", RoleCode.MEMBER));

        CurrentActor secondAdmin = tenantAdmin(secondTenant.getId());
        PageResponse<UserAdministrationService.UserSummary> page = userAdministration.list(
                secondAdmin, PageRequest.of(0, 20));

        assertThat(page.content()).extracting(UserAdministrationService.UserSummary::id)
                .containsExactly(secondUser.getId());
        assertThat(users.findByTenantIdAndId(secondTenant.getId(), firstUser.getId())).isEmpty();

        assertThatThrownBy(() -> userAdministration.changeStatus(
                secondAdmin, firstUser.getId(), UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThatThrownBy(() -> userAdministration.replaceRoles(
                secondAdmin, firstUser.getId(), Set.of(RoleCode.COACH)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(firstUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void platformAdminCannotEnterTenantUserManagement() {
        CurrentActor platformAdmin = new CurrentActor(
                1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of("platform:tenant:read"), 0L, "platform-token");

        assertThatThrownBy(() -> userAdministration.list(platformAdmin, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> userAdministration.changeStatus(platformAdmin, 1L, UserStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    private static CurrentActor tenantAdmin(Long tenantId) {
        return new CurrentActor(
                2L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("user:read", "user:write", "role:assign"), 0L, "admin-token");
    }
}
