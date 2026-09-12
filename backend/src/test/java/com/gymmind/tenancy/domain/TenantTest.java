package com.gymmind.tenancy.domain;

import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.model.TenantStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    @Test
    void createNormalizesCodeAndStartsActive() {
        Tenant tenant = Tenant.create("  Power-House ", "Power House");

        assertThat(tenant.getCode()).isEqualTo("power-house");
        assertThat(tenant.getName()).isEqualTo("Power House");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.isActive()).isTrue();
    }

    @Test
    void disabledTenantIsNotActive() {
        Tenant tenant = Tenant.create("power-house", "Power House");

        tenant.disable();

        assertThat(tenant.isActive()).isFalse();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.DISABLED);
    }

    @Test
    void lifecycleOperationsAreIdempotent() {
        Tenant tenant = Tenant.create("power-house", "Power House");

        tenant.disable();
        tenant.disable();
        assertThat(tenant.isActive()).isFalse();

        tenant.activate();
        tenant.activate();
        assertThat(tenant.isActive()).isTrue();
    }

    @Test
    void createRejectsBlankCodeAndName() {
        assertThatThrownBy(() -> Tenant.create(" ", "Power House"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Tenant.create("power-house", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
