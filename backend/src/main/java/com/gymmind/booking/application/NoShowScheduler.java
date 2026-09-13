package com.gymmind.booking.application;

import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NoShowScheduler {
    private final TenantRepository tenants;
    private final NoShowService noShows;

    public NoShowScheduler(TenantRepository tenants, NoShowService noShows) {
        this.tenants = tenants;
        this.noShows = noShows;
    }

    @Scheduled(fixedDelayString = "${gymmind.booking.no-show-delay-ms:60000}")
    public void run() {
        runOnce(Instant.now());
    }

    void runOnce(Instant now) {
        tenants.findAll(Pageable.unpaged()).getContent().stream()
                .filter(tenant -> tenant.isActive() && tenant.getId() != null)
                .forEach(tenant -> noShows.markTenantNoShows(tenant.getId(), now));
    }
}
