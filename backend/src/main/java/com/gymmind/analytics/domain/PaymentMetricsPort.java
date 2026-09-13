package com.gymmind.analytics.domain;
import java.math.BigDecimal; import java.time.LocalDate;
public interface PaymentMetricsPort { BigDecimal amount(Long tenantId, LocalDate from, LocalDate to); }
