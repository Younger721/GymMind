package com.gymmind.analytics.domain;
import java.time.LocalDate;
public interface BookingMetricsPort { long total(Long tenantId, LocalDate from, LocalDate to); long checkedIn(Long tenantId, LocalDate from, LocalDate to); }
