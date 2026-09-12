package com.gymmind.tenancy.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.application.port.TenantProvisioningContributor;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantSettingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TenantProvisioningServiceIT.FailingContributorConfiguration.class)
class TenantProvisioningServiceIT extends MySqlIntegrationTest {

    @Autowired private TenantProvisioningService provisioningService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @PersistenceContext private EntityManager entityManager;

    @Test
    @Transactional
    void provisionsTenantAdministratorRoleAndSettings() {
        TenantProvisioningService.ProvisionedTenant provisioned = provisioningService.provision(
                Tenant.create("success-gym", "Success Gym"),
                new TenantProvisioningService.Administrator(
                        "success@example.com", "$2a$10$already-hashed", "Owner"));
        entityManager.flush();

        assertThat(count("Tenant")).isEqualTo(1L);
        assertThat(count("UserAccount")).isEqualTo(1L);
        assertThat(count("UserRole")).isEqualTo(1L);
        assertThat(count("TenantSettings")).isEqualTo(1L);
        assertThat(provisioned.administrator().getTenantId()).isEqualTo(provisioned.tenant().getId());
        assertThat(provisioned.administrator().getPasswordHash()).isEqualTo("$2a$10$already-hashed");
        assertThat(provisioned.administrator().getDisplayName()).isEqualTo("Owner");
        Long gymAdminRoleId = roleRepository.findByCode(RoleCode.GYM_ADMIN).orElseThrow().getId();
        assertThat(userRoleRepository.existsByUserIdAndRoleId(
                provisioned.administrator().getId(), gymAdminRoleId)).isTrue();
    }

    @Test
    void contributorFailureRollsBackTenantUserRoleAndSettings() {
        assertThatThrownBy(() -> provisioningService.provision(
                Tenant.create("rollback-gym", "Rollback Gym"),
                new TenantProvisioningService.Administrator(
                        "rollback@example.com", "$2a$10$already-hashed", "Owner")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("contributor failed");

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
        TenantProvisioningContributor failingContributor(TenantSettingsRepository settingsRepository) {
            return tenant -> {
                if ("rollback-gym".equals(tenant.getCode())) {
                    assertThat(settingsRepository.findByTenantId(tenant.getId())).isPresent();
                    throw new IllegalStateException("contributor failed");
                }
            };
        }
    }
}
