package com.gymmind.analytics.application;

import com.gymmind.analytics.domain.*;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {
  @Test void dashboardAggregatesDeterministicallyWithinTenantAndPeriod() {
    var members=mock(MemberMetricsPort.class); var bookings=mock(BookingMetricsPort.class); var payments=mock(PaymentMetricsPort.class); var memberships=mock(MembershipMetricsPort.class);
    when(members.total(7L)).thenReturn(10L); when(members.active(7L)).thenReturn(8L); when(bookings.total(7L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,7))).thenReturn(20L); when(bookings.checkedIn(7L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,7))).thenReturn(15L); when(payments.amount(7L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,7))).thenReturn(new BigDecimal("123.45")); when(memberships.renewals(7L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,7))).thenReturn(6L); when(memberships.renewalCandidates(7L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,7))).thenReturn(8L);
    var s=new DefaultDashboardQueryService(members,bookings,payments,memberships); var d=s.dashboard(actor(7L),LocalDate.of(2026,9,1),LocalDate.of(2026,9,7));
    assertThat(d.memberCount()).isEqualTo(10); assertThat(d.activeRate()).isEqualByComparingTo("0.8"); assertThat(d.bookingRate()).isEqualByComparingTo("0.75"); assertThat(d.checkInRate()).isEqualByComparingTo("0.75"); assertThat(d.recordedRevenue()).isEqualByComparingTo("123.45"); assertThat(d.renewalRate()).isEqualByComparingTo("0.75"); verify(members).total(7L); verify(bookings).total(7L,LocalDate.of(2026,9,1),LocalDate.of(2026,9,7));
  }
  @Test void platformAdminGetsSummaryAndCannotDrillDown() { var s=new DefaultDashboardQueryService(mock(MemberMetricsPort.class),mock(BookingMetricsPort.class),mock(PaymentMetricsPort.class),mock(MembershipMetricsPort.class)); assertThatThrownBy(()->s.dashboard(platform(),LocalDate.now().minusDays(1),LocalDate.now())).isInstanceOf(IllegalArgumentException.class); }
  private static CurrentActor actor(long t){return new CurrentActor(1L,t,Set.of(RoleCode.GYM_ADMIN),Set.of("analytics:read"),0,"tok");}
  private static CurrentActor platform(){return new CurrentActor(1L,null,Set.of(RoleCode.PLATFORM_ADMIN),Set.of("platform:analytics:read"),0,"tok");}
}
