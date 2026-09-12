package com.gymmind.tenancy.infrastructure;

import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TenantRepositoryIT extends MySqlIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndFindsTenantByNormalizedCode() {
        Tenant saved = tenantRepository.save(Tenant.create("  POWER-HOUSE ", "Power House"));
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isPositive();
        assertThat(tenantRepository.findByCode("power-house")).get()
                .extracting(Tenant::getName).isEqualTo("Power House");
        assertThat(tenantRepository.existsByCode("power-house")).isTrue();
    }

    @Test
    void tenantCodeIsGloballyUnique() {
        tenantRepository.save(Tenant.create("power-house", "Power House"));
        entityManager.flush();

        assertThatThrownBy(() -> tenantRepository.save(Tenant.create("POWER-HOUSE", "Another House")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
