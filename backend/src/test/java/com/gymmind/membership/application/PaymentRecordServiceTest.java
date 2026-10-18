package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.membership.domain.model.Order;
import com.gymmind.membership.domain.model.OrderStatus;
import com.gymmind.membership.domain.model.PaymentRecord;
import com.gymmind.membership.domain.repository.OrderRepository;
import com.gymmind.membership.domain.repository.PaymentRecordRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRecordServiceTest {

    @Test
    void recordingPaymentDoesNotChangeOrderStatusAndIsIdempotentByReference() {
        PaymentRecordRepository payments = mock(PaymentRecordRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        AuditRecorder audit = mock(AuditRecorder.class);
        Order order = placedOrder(11L, 8L, "99.00", "CNY");
        when(orders.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(order));
        when(payments.findByTenantIdAndReference(11L, "R-1")).thenReturn(Optional.empty());
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any(PaymentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultPaymentRecordService service = new DefaultPaymentRecordService(payments, orders, audit);

        PaymentRecordView result = service.recordOfflinePayment(admin(11L), payment("99.00", "CNY"));

        assertThat(result.reference()).isEqualTo("R-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        verify(orders, never()).save(any(Order.class));
        verify(payments).save(any(PaymentRecord.class));

        PaymentRecord existing = PaymentRecord.create(11L, 8L,
                new BigDecimal("99.00"), "CNY", "R-1", "CASH");
        when(payments.findByTenantIdAndReference(11L, "R-1")).thenReturn(Optional.of(existing));
        PaymentRecordView repeated = service.recordOfflinePayment(admin(11L), payment("99.00", "CNY"));

        assertThat(repeated.reference()).isEqualTo("R-1");
        verify(payments, org.mockito.Mockito.times(1)).save(any(PaymentRecord.class));
    }

    @Test
    void recordsAmountThatDiffersFromOrderTotalWithoutChangingOrder() {
        PaymentRecordRepository payments = mock(PaymentRecordRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        Order order = placedOrder(11L, 8L, "99.00", "CNY");
        when(payments.findByTenantIdAndReference(11L, "R-1")).thenReturn(Optional.empty());
        when(orders.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(order));
        when(payments.save(any(PaymentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultPaymentRecordService service = new DefaultPaymentRecordService(
                payments, orders, mock(AuditRecorder.class));

        PaymentRecordView result = service.recordOfflinePayment(admin(11L), payment("98.00", "CNY"));

        assertThat(result.amount()).isEqualByComparingTo("98.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        verify(orders, never()).save(any(Order.class));
        verify(payments).save(any(PaymentRecord.class));
    }

    private static Order placedOrder(long tenantId, long memberId, String amount, String currency) {
        Order order = Order.create(tenantId, memberId, new BigDecimal(amount), currency);
        order.place();
        return order;
    }

    private static RecordOfflinePaymentCommand payment(String amount, String currency) {
        return new RecordOfflinePaymentCommand(null, 8L, new BigDecimal(amount), currency,
                "R-1", "CASH");
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("payment:write"), 0L, "token");
    }
}
