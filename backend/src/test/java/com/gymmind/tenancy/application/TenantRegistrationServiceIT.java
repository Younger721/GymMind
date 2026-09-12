package com.gymmind.tenancy.application;

import com.gymmind.iam.application.AuthApplicationService;
import com.gymmind.iam.application.command.RegisterTenantCommand;
import com.gymmind.iam.domain.model.Permission;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.repository.PermissionRepository;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.application.port.TenantProvisioningContributor;
import com.gymmind.tenancy.domain.repository.TenantSettingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TenantRegistrationServiceIT.FailingContributorConfiguration.class)
class TenantRegistrationServiceIT extends MySqlIntegrationTest {

    @Autowired private AuthApplicationService authService;
    @Autowired private TenantRegistrationService registrationService;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @PersistenceContext private EntityManager entityManager;

    @Test
    @Transactional
    void registrationNormalizesEmailHashesPasswordAndPersistsAuthoritativeGymAdminRole() {
        Role gymAdmin = roleRepository.findByCode(RoleCode.GYM_ADMIN).orElseThrow();
        Permission permission = permissionRepository.findByCode("tenant:settings:write").orElseThrow();
        entityManager.persist(RolePermission.link(gymAdmin, permission));

        TenantProvisioningService.ProvisionedTenant provisioned = registrationService.register(
                new RegisterTenantCommand("new-gym", "New Gym", " Owner@Example.COM ",
                        "correct-password", "Owner"));
        entityManager.flush();

        assertThat(provisioned.administrator().getNormalizedEmail()).isEqualTo("owner@example.com");
        assertThat(new BCryptPasswordEncoder().matches(
                "correct-password", provisioned.administrator().getPasswordHash())).isTrue();
        assertThat(userRepository.findById(provisioned.administrator().getId())).isPresent();
        assertThat(userRoleRepository.findRoleCodesByUserId(provisioned.administrator().getId()))
                .containsExactly(RoleCode.GYM_ADMIN);
        assertThat(userRoleRepository.findPermissionCodesByUserId(provisioned.administrator().getId()))
                .containsExactly("tenant:settings:write");
    }

    @Test
    void contributorFailureRollsBackPublicRegistrationTenantUserRoleAndSettings() {
        assertThatThrownBy(() -> authService.registerTenant(new RegisterTenantCommand(
                "registration-rollback", "Rollback Gym", "owner@example.com", "password", "Owner")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("registration contributor failed");

        assertThat(count("Tenant")).isZero();
        assertThat(count("UserAccount")).isZero();
        assertThat(count("UserRole")).isZero();
        assertThat(count("TenantSettings")).isZero();
    }

    private long count(String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class).getSingleResult();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingContributorConfiguration {
        @Bean
        @Order(1000)
        TenantProvisioningContributor registrationFailingContributor(TenantSettingsRepository settingsRepository) {
            return tenant -> {
                if ("registration-rollback".equals(tenant.getCode())) {
                    assertThat(settingsRepository.findByTenantId(tenant.getId())).isPresent();
                    throw new IllegalStateException("registration contributor failed");
                }
            };
        }
    }
}
