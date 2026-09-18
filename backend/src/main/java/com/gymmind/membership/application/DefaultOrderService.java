package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.membership.domain.model.Order;
import com.gymmind.membership.domain.repository.OrderRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DefaultOrderService {

    private final OrderRepository orders;
    private final AuditRecorder audit;

    public DefaultOrderService(OrderRepository orders, AuditRecorder audit) {
        this.orders = orders;
        this.audit = audit;
    }

    @Transactional
    public OrderView create(CurrentActor actor, CreateOrderCommand command) {
        ActorAccess.requireGymAdmin(actor, "order:write");
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Order order = orders.save(Order.create(tenantId, command.memberId(), command.total(), command.currency()));
        audit.record(new AuditEvent("ORDER_CREATED", "ORDER", order.getId(),
                AuditResult.SUCCESS, "order-service", Map.of()));
        return OrderView.from(order);
    }
}
