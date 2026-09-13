package com.gymmind.iam.api;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserTenantIsolationIT extends MySqlIntegrationTest {
    @Autowired
    private UserAccountRepository users;

    @Autowired
    private TenantRepository tenants;

    @Test
    void tenantScopedLookupCannotReturnAnotherTenantUser() {
        Tenant first = tenants.save(Tenant.create("isolated-a", "Isolated A"));
        tenants.save(Tenant.create("isolated-b", "Isolated B"));
        UserAccount user = users.save(UserAccount.tenantUser(first.getId(), "isolated@example.com", "hash", "Member", RoleCode.MEMBER));
        assertThat(users.findByTenantIdAndId(first.getId() + 1, user.getId())).isEmpty();
    }
}
