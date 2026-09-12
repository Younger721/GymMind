package com.gymmind.iam.infrastructure;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import com.gymmind.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserAccountRepositoryIT extends MySqlIntegrationTest {

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndFindsByNormalizedEmailAndTenant() {
        Long tenantId = tenantRepository.save(Tenant.create("coach-gym", "Coach Gym")).getId();
        UserAccount saved = userRepository.save(UserAccount.tenantUser(tenantId, "  Coach@Example.COM ", "$hash", "Coach"));
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isPositive();
        assertThat(userRepository.findByNormalizedEmail("coach@example.com")).get()
                .extracting(UserAccount::getTenantId).isEqualTo(tenantId);
        assertThat(userRepository.findByTenantIdAndId(tenantId, saved.getId())).isPresent();
        assertThat(userRepository.findByTenantIdAndId(tenantId + 1, saved.getId())).isEmpty();
        assertThat(userRepository.findAllByTenantId(tenantId, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void normalizedEmailIsGloballyUniqueAcrossTenants() {
        Long firstTenantId = tenantRepository.save(Tenant.create("first-gym", "First Gym")).getId();
        Long secondTenantId = tenantRepository.save(Tenant.create("second-gym", "Second Gym")).getId();
        userRepository.save(UserAccount.tenantUser(firstTenantId, "member@example.com", "$hash", "One"));
        entityManager.flush();

        assertThatThrownBy(() -> userRepository.save(
                UserAccount.tenantUser(secondTenantId, " MEMBER@EXAMPLE.COM ", "$hash", "Two")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
