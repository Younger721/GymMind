package com.gymmind.analytics.domain;
import java.time.LocalDate;
public interface MembershipMetricsPort { long renewals(Long tenantId, LocalDate from, LocalDate to); long renewalCandidates(Long tenantId, LocalDate from, LocalDate to); }
