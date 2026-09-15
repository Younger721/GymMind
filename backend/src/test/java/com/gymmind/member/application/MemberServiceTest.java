package com.gymmind.member.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.model.MemberStatus;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceTest {

    @Test
    void createUsesActorTenantAndRecordsAudit() {
        MemberRepository repository = mock(MemberRepository.class);
        AuditRecorder audit = mock(AuditRecorder.class);
        when(repository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultMemberService service = new DefaultMemberService(repository, audit);

        MemberView view = service.create(admin(11L), new CreateMemberCommand(null, "M-001", "Alice", "13800000000", 21L));

        assertThat(view.tenantId()).isEqualTo(11L);
        assertThat(view.userId()).isEqualTo(21L);
        verify(audit).record(any());
    }

    @Test
    void memberCanOnlyReadOwnProfileAndCrossTenantLookupIsNotAccepted() {
        MemberRepository repository = mock(MemberRepository.class);
        when(repository.findByTenantIdAndUserId(11L, 21L)).thenReturn(Optional.of(Member.create(11L, 21L, "M-001", "Alice", "13800000000")));
        DefaultMemberService service = new DefaultMemberService(repository, mock(AuditRecorder.class));

        assertThat(service.findSelf(member(11L, 21L)).userId()).isEqualTo(21L);
        assertThatThrownBy(() -> service.findSelf(member(12L, 21L)))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        verify(repository).findByTenantIdAndUserId(11L, 21L);
    }

    @Test
    void suspendChangesOnlyTenantScopedRecord() {
        MemberRepository repository = mock(MemberRepository.class);
        Member member = Member.create(11L, 21L, "M-001", "Alice", "13800000000");
        when(repository.findByTenantIdAndId(11L, 5L)).thenReturn(Optional.of(member));
        when(repository.save(member)).thenReturn(member);
        DefaultMemberService service = new DefaultMemberService(repository, mock(AuditRecorder.class));

        service.suspend(admin(11L), 5L);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        verify(repository).findByTenantIdAndId(11L, 5L);
    }

    @Test
    void listSearchUsesActorTenantAndMapsPage() {
        MemberRepository repository = mock(MemberRepository.class);
        Member member = Member.create(11L, 21L, "M-001", "Alice", "13800000000");
        when(repository.searchByTenantId(11L, "Ali", Pageable.ofSize(20)))
                .thenReturn(new PageImpl<>(java.util.List.of(member), Pageable.ofSize(20), 1));
        DefaultMemberService service = new DefaultMemberService(repository, mock(AuditRecorder.class));

        var page = service.list(admin(11L), "Ali", Pageable.ofSize(20));

        assertThat(page.content()).extracting(MemberView::fullName).containsExactly("Alice");
        verify(repository).searchByTenantId(11L, "Ali", Pageable.ofSize(20));
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("member:read", "member:write"), 0L, "token");
    }

    private static CurrentActor member(long tenantId, long userId) {
        return new CurrentActor(userId, tenantId, Set.of(RoleCode.MEMBER), Set.of("member:read"), 0L, "token");
    }
}
