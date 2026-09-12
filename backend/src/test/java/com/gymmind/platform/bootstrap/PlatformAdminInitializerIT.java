package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PlatformAdminInitializerIT extends MySqlIntegrationTest {

    @DynamicPropertySource
    static void platformAdminProperties(DynamicPropertyRegistry registry) {
        registry.add("PLATFORM_ADMIN_EMAIL", () -> "root@example.com");
        registry.add("PLATFORM_ADMIN_PASSWORD", () -> "change-me-now");
    }

    @Autowired private PlatformAdminInitializer initializer;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PlatformAdminProperties properties;
    @PersistenceContext private EntityManager entityManager;

    @Test
    void platformAdminIsCreatedWithBcryptPasswordAndRoleOnce() {
        assertThat(userRepository.findByNormalizedEmail("root@example.com")).isPresent();
        String originalPasswordHash = userRepository.findByNormalizedEmail("root@example.com")
                .orElseThrow()
                .getPasswordHash();

        initializer.run();
        initializer.run();
        entityManager.flush();
        entityManager.clear();

        UserAccount user = userRepository.findByNormalizedEmail("root@example.com").orElseThrow();
        assertThat(user.getTenantId()).isNull();
        assertThat(user.getPasswordHash()).isEqualTo(originalPasswordHash);
        assertThat(user.getPasswordHash()).isNotEqualTo("change-me-now");
        assertThat(new BCryptPasswordEncoder().matches("change-me-now", user.getPasswordHash())).isTrue();

        Long roleId = roleRepository.findByCode(RoleCode.PLATFORM_ADMIN).orElseThrow().getId();
        assertThat(userRoleRepository.existsByUserIdAndRoleId(user.getId(), roleId)).isTrue();
        assertThat(userRoleRepository.countByUserId(user.getId())).isEqualTo(1L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void configuredEmailOwnedByTenantUserFailsClosed() {
        Long tenantId = tenantRepository.save(Tenant.create("admin-collision", "Admin Collision")).getId();
        UserAccount tenantUser = userRepository.save(UserAccount.tenantUser(
                tenantId, "collision@example.com", "$2a$10$existing-hash", "Tenant User"));

        String configuredEmail = properties.getEmail();
        try {
            properties.setEmail("collision@example.com");

            assertThatThrownBy(initializer::run)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("tenant user");
            assertThat(userRoleRepository.countByUserId(tenantUser.getId())).isZero();
        } finally {
            properties.setEmail(configuredEmail);
        }
    }
}
