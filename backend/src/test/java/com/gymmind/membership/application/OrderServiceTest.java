package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.membership.domain.model.Order;
import com.gymmind.membership.domain.repository.OrderRepository;
import com.gymmind.shared.error.BusinessException;
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

class OrderServiceTest {

    @Test
    void createOrderUsesActorTenantAfterValidatingMember() {
        OrderRepository orders = mock(OrderRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.of(mock(Member.class)));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultOrderService service = new DefaultOrderService(orders, members, mock(AuditRecorder.class));

        OrderView view = service.create(admin(11L), new CreateOrderCommand(null, 41L,
                new BigDecimal("99"), "CNY"));

        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.memberId()).isEqualTo(41L);
        verify(members).findByTenantIdAndId(11L, 41L);
    }

    @Test
    void createOrderRejectsMemberOutsideCurrentTenant() {
        OrderRepository orders = mock(OrderRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.empty());
        DefaultOrderService service = new DefaultOrderService(orders, members, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(11L), new CreateOrderCommand(null, 41L,
                new BigDecimal("99"), "CNY"))).isInstanceOf(BusinessException.class);

        verify(orders, never()).save(any(Order.class));
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("order:write"), 0L, "token");
    }
}
