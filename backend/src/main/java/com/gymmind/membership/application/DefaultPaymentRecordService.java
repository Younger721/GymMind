package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.membership.domain.model.PaymentRecord;
import com.gymmind.membership.domain.repository.OrderRepository;
import com.gymmind.membership.domain.repository.PaymentRecordRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DefaultPaymentRecordService {

    private final PaymentRecordRepository payments;
    private final OrderRepository orders;
    private final AuditRecorder audit;

    public DefaultPaymentRecordService(
            PaymentRecordRepository payments, OrderRepository orders, AuditRecorder audit) {
        this.payments = payments;
        this.orders = orders;
        this.audit = audit;
    }

    @Transactional
    public PaymentRecordView recordOfflinePayment(CurrentActor actor, RecordOfflinePaymentCommand command) {
        ActorAccess.requireGymAdmin(actor, "payment:write");
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return payments.findByTenantIdAndReference(tenantId, command.reference())
                .map(PaymentRecordView::from)
                .orElseGet(() -> {
                    PaymentRecord payment = payments.save(PaymentRecord.create(
                            tenantId, command.orderId(), command.amount(), command.currency(),
                            command.reference(), command.paymentMethod()));
                    audit.record(new AuditEvent("PAYMENT_RECORDED", "PAYMENT", payment.getId(),
                            AuditResult.SUCCESS, "payment-service", Map.of()));
                    return PaymentRecordView.from(payment);
                });
    }
}
