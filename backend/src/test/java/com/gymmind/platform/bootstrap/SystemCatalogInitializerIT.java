package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.application.TenantProvisioningService;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantSettingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SystemCatalogInitializerIT extends MySqlIntegrationTest {

    @Autowired private SystemCatalogInitializer initializer;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private TenantProvisioningService provisioningService;
    @Autowired private TenantSettingsRepository tenantSettingsRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @PersistenceContext private EntityManager entityManager;

    @Test
    void catalogInitializationIsIdempotent() {
        assertCatalogContents();

        initializer.run();
        initializer.run();

        assertCatalogContents();
        assertThat(entityManager.createQuery("select count(r) from Role r", Long.class).getSingleResult())
                .isEqualTo(4L);
    }

    private void assertCatalogContents() {
        assertThat(roleRepository.count()).isEqualTo(4);
        List<RoleCode> requiredRoles = List.of(
                RoleCode.PLATFORM_ADMIN, RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER);
        assertThat(RoleCode.values()).containsExactlyInAnyOrderElementsOf(requiredRoles);
        assertThat(requiredRoles).allSatisfy(code -> assertThat(roleRepository.findByCode(code)).isPresent());

        List<String> requiredPermissions = List.of(
                "platform:tenant:read",
                "platform:tenant:write",
                "tenant:settings:read",
                "tenant:settings:write",
                "user:read",
                "user:write",
                "role:read",
                "role:assign");
        assertThat(permissionRepository.count()).isEqualTo(8);
        assertThat(requiredPermissions).allSatisfy(
                code -> assertThat(permissionRepository.findByCode(code)).isPresent());
    }

    @Test
    void tenantProvisioningCreatesSettingsAndGymAdminRole() {
        initializer.run();

        TenantProvisioningService.ProvisionedTenant provisioned = provisioningService.provision(
                Tenant.create("new-gym", "New Gym"),
                new TenantProvisioningService.Administrator(
                        "owner@example.com", "$2a$10$already-hashed", "Owner"));
        entityManager.flush();

        Long gymAdminRoleId = roleRepository.findByCode(RoleCode.GYM_ADMIN).orElseThrow().getId();
        assertThat(tenantSettingsRepository.findByTenantId(provisioned.tenant().getId())).isPresent();
        assertThat(provisioned.administrator().getTenantId()).isEqualTo(provisioned.tenant().getId());
        assertThat(userRoleRepository.existsByUserIdAndRoleId(
                provisioned.administrator().getId(), gymAdminRoleId)).isTrue();
    }
}
