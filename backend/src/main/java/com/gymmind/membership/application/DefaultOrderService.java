package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.member.domain.repository.MemberRepository;
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
    private final MemberRepository members;
    private final AuditRecorder audit;

    public DefaultOrderService(OrderRepository orders, MemberRepository members, AuditRecorder audit) {
        this.orders = orders;
        this.members = members;
        this.audit = audit;
    }

    @Transactional
    public OrderView create(CurrentActor actor, CreateOrderCommand command) {
        ActorAccess.requireGymAdmin(actor, "order:write");
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        members.findByTenantIdAndId(tenantId, command.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Order order = Order.create(tenantId, command.memberId(), command.total(), command.currency());
        order.place();
        order = orders.save(order);
        audit.record(new AuditEvent("ORDER_CREATED", "ORDER", order.getId(),
                AuditResult.SUCCESS, "order-service", Map.of()));
        return OrderView.from(order);
    }
}
