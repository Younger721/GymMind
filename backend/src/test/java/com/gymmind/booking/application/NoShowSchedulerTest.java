package com.gymmind.booking.application;

import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NoShowSchedulerTest {
    @Test
    void runsNoShowProcessingOnlyForActiveTenants() {
        TenantRepository tenants = mock(TenantRepository.class);
        NoShowService noShows = mock(NoShowService.class);
        Tenant active = mock(Tenant.class);
        Tenant disabled = mock(Tenant.class);
        when(active.isActive()).thenReturn(true);
        when(active.getId()).thenReturn(11L);
        when(disabled.isActive()).thenReturn(false);
        when(disabled.getId()).thenReturn(12L);
        when(tenants.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(active, disabled)));

        new NoShowScheduler(tenants, noShows).runOnce(Instant.parse("2026-01-01T00:00:00Z"));

        verify(noShows).markTenantNoShows(11L, Instant.parse("2026-01-01T00:00:00Z"));
        verify(noShows, never()).markTenantNoShows(eq(12L), any(Instant.class));
    }
}
