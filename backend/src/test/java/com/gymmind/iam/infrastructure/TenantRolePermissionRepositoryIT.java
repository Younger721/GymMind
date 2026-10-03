package com.gymmind.iam.infrastructure;

import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.model.TenantRolePermission;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.TenantRolePermissionRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TenantRolePermissionRepositoryIT extends MySqlIntegrationTest {
    @Autowired private TenantRolePermissionRepository tenantLinks;
    @Autowired private UserRoleRepository userRoles;
    @Autowired private RoleRepository roles;
    @Autowired private PermissionRepository permissions;
    @Autowired private UserAccountRepository users;
    @Autowired private TenantRepository tenants;
    @PersistenceContext private EntityManager entityManager;

    @Test
    void tenantOverrideTakesPrecedenceAndDoesNotCrossTenant() {
        Role coach = roles.findByCode(RoleCode.COACH).orElseThrow();
        Permission global = permissions.save(Permission.of("tenant-override:global", "Global"));
        Permission local = permissions.save(Permission.of("tenant-override:local", "Local"));
        Permission marker = permissions.findByCode("role:menu:configured").orElseThrow();
        Tenant first = tenants.save(Tenant.create("override-first", "Override First"));
        Tenant second = tenants.save(Tenant.create("override-second", "Override Second"));
        UserAccount firstUser = users.save(UserAccount.tenantUser(first.getId(), "override-first@example.com", "override-first", "$hash", "First", null, RoleCode.COACH));
        UserAccount secondUser = users.save(UserAccount.tenantUser(second.getId(), "override-second@example.com", "override-second", "$hash", "Second", null, RoleCode.COACH));
        entityManager.flush();
        entityManager.persist(RolePermission.link(coach, global));
        entityManager.persist(UserRole.assign(firstUser, coach));
        entityManager.persist(UserRole.assign(secondUser, coach));
        entityManager.flush();

        tenantLinks.save(TenantRolePermission.link(first, coach, local));
        tenantLinks.save(TenantRolePermission.link(first, coach, marker));
        entityManager.flush();

        assertThat(userRoles.findPermissionCodesByUserId(firstUser.getId(), first.getId()))
                .contains("tenant-override:local").doesNotContain("tenant-override:global");
        assertThat(userRoles.findPermissionCodesByUserId(secondUser.getId(), second.getId()))
                .contains("tenant-override:global").doesNotContain("tenant-override:local");
    }
}
