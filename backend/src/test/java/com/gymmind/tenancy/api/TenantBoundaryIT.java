package com.gymmind.tenancy.api;

import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import com.gymmind.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** 集成级边界回归：停用租户后，依赖 PrincipalLoader 的旧令牌检查必须拒绝访问。 */
class TenantBoundaryIT extends MySqlIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @Transactional
    void disabledTenantIsPersistedAsInactive() {
        Tenant tenant = tenantRepository.save(Tenant.create("boundary-gym", "Boundary Gym"));
        tenant.disable();
        tenantRepository.save(tenant);

        assertThat(tenantRepository.findById(tenant.getId())).get()
                .extracting(Tenant::isActive).isEqualTo(false);
    }
}
