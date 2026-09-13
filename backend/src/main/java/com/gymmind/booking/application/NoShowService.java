package com.gymmind.booking.application;

import java.time.Instant;

public interface NoShowService {
    int markTenantNoShows(Long tenantId, Instant now);
}
