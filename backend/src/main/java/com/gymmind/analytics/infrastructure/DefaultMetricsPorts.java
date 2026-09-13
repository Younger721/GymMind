package com.gymmind.analytics.infrastructure;
import com.gymmind.analytics.domain.*; import org.springframework.stereotype.Component; import java.math.BigDecimal; import java.time.LocalDate;
@Component class DefaultMemberMetricsPort implements MemberMetricsPort { public long total(Long t){return 0;} public long active(Long t){return 0;} }
@Component class DefaultBookingMetricsPort implements BookingMetricsPort { public long total(Long t,LocalDate f,LocalDate to){return 0;} public long checkedIn(Long t,LocalDate f,LocalDate to){return 0;} }
@Component class DefaultPaymentMetricsPort implements PaymentMetricsPort { public BigDecimal amount(Long t,LocalDate f,LocalDate to){return BigDecimal.ZERO;} }
@Component class DefaultMembershipMetricsPort implements MembershipMetricsPort { public long renewals(Long t,LocalDate f,LocalDate to){return 0;} public long renewalCandidates(Long t,LocalDate f,LocalDate to){return 0;} }
