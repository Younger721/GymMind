package com.gymmind.tenancy.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.TenantContextHolder;
import com.gymmind.tenancy.application.command.UpdateTenantSettingsCommand;
import com.gymmind.tenancy.domain.model.TenantSettings;
import com.gymmind.tenancy.domain.repository.TenantSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTenantSettingsServiceTest {

    private TenantSettingsRepository settingsRepository;
    private DefaultTenantSettingsService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        settingsRepository = mock(TenantSettingsRepository.class);
        service = new DefaultTenantSettingsService(settingsRepository);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void platformAdminReadsAndUpdatesOnlyTheSelectedTenantSettings() {
        TenantSettings current = TenantSettings.create(10L);
        when(settingsRepository.findByTenantId(10L)).thenReturn(Optional.of(current));
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TenantContextHolder.set(10L);

        var read = service.getCurrent(platformAdmin());
        var updated = service.updateCurrent(platformAdmin(),
                new UpdateTenantSettingsCommand(false, true, "Asia/Shanghai"));

        assertThat(read.tenantId()).isEqualTo(10L);
        assertThat(updated.tenantId()).isEqualTo(10L);
        assertThat(updated.reservationEnabled()).isFalse();
        assertThat(updated.timezone()).isEqualTo("Asia/Shanghai");
        verify(settingsRepository, times(2)).findByTenantId(10L);
    }

    @Test
    void platformAdminCannotReadSettingsWithoutSelectedTenant() {
        assertThatThrownBy(() -> service.getCurrent(platformAdmin()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TENANT_CONTEXT_REQUIRED);
        verify(settingsRepository, never()).findByTenantId(any());
    }
}
