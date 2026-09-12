package com.gymmind.tenancy.infrastructure;

import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.model.TenantSettings;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import com.gymmind.tenancy.domain.repository.TenantSettingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TenantSettingsRepositoryIT extends MySqlIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantSettingsRepository settingsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndFindsSettingsByTenantId() {
        Tenant tenant = tenantRepository.save(Tenant.create("power-house", "Power House"));
        entityManager.flush();
        TenantSettings settings = TenantSettings.create(tenant.getId());
        settings.setReservationEnabled(false);
        settings.setCheckInEnabled(true);
        settings.setTimezone("Asia/Shanghai");

        settingsRepository.save(settings);
        entityManager.flush();
        entityManager.clear();

        assertThat(settingsRepository.findByTenantId(tenant.getId())).get()
                .satisfies(found -> {
                    assertThat(found.isReservationEnabled()).isFalse();
                    assertThat(found.isCheckInEnabled()).isTrue();
                    assertThat(found.getTimezone()).isEqualTo("Asia/Shanghai");
                });
    }

    @Test
    void settingsTenantIdIsUniqueAndNotNullable() {
        Tenant tenant = tenantRepository.save(Tenant.create("power-house", "Power House"));
        entityManager.flush();

        settingsRepository.save(TenantSettings.create(tenant.getId()));
        entityManager.flush();
        assertThatThrownBy(() -> settingsRepository.save(TenantSettings.create(tenant.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void settingsTenantIdCannotBeNull() {
        assertThatThrownBy(() -> settingsRepository.save(TenantSettings.create(null)))
                .isInstanceOfAny(IllegalArgumentException.class, DataIntegrityViolationException.class);
    }
}
