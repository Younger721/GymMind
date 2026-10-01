package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MembershipPackage;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.membership.domain.repository.MembershipPackageRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MemberMembershipServiceTest {
    @Test
    void grantCreatesTenantScopedEntitlement() {
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        MembershipPackageRepository packages = mock(MembershipPackageRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        when(packages.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(
                MembershipPackage.create(11L, "10次", "COUNT", 10, null, new BigDecimal("99"), "CNY")));
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.of(mock(com.gymmind.member.domain.model.Member.class)));
        when(memberships.save(any(MemberMembership.class))).thenAnswer(i -> i.getArgument(0));

        MemberMembershipService service = new DefaultMemberMembershipService(
                memberships, packages, members, mock(AuditRecorder.class));
        MemberMembershipView view = service.grant(
                admin(11L), new GrantMembershipCommand(null, 41L, 8L, Instant.now(), null));

        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.remainingCount()).isEqualTo(10);
        verify(memberships).save(any(MemberMembership.class));
    }

    @Test
    void grantRejectsMemberFromAnotherTenant() {
        MemberMembershipRepository memberships = mock(MemberMembershipRepository.class);
        MembershipPackageRepository packages = mock(MembershipPackageRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        when(packages.findByTenantIdAndId(11L, 8L)).thenReturn(Optional.of(
                MembershipPackage.create(11L, "10次", "COUNT", 10, null, new BigDecimal("99"), "CNY")));
        when(members.findByTenantIdAndId(11L, 41L)).thenReturn(Optional.empty());

        MemberMembershipService service = new DefaultMemberMembershipService(
                memberships, packages, members, mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.grant(
                admin(11L), new GrantMembershipCommand(null, 41L, 8L, Instant.now(), null)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(memberships);
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("membership:write"), 0L, "token");
    }
}
