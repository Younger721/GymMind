package com.gymmind.member.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantAccessGuard;
import com.gymmind.iam.domain.model.RoleCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import com.gymmind.shared.api.PageResponse;

@Service
public class DefaultMemberService implements MemberService {
    private final MemberRepository members;
    private final AuditRecorder audit;
    private final TenantAccessGuard guard = new TenantAccessGuard();

    public DefaultMemberService(MemberRepository members, AuditRecorder audit) {
        this.members = Objects.requireNonNull(members, "members");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    @Transactional
    public MemberView create(CurrentActor actor, CreateMemberCommand command) {
        requireAdmin(actor, "member:write");
        Objects.requireNonNull(command, "command");
        Long tenantId = guard.requireTenant(actor);
        if (command.tenantId() != null && !tenantId.equals(command.tenantId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (members.findByTenantIdAndMemberNumber(tenantId, command.memberNumber()).isPresent()
                || members.existsByTenantIdAndPhone(tenantId, command.phone())) throw new BusinessException(ErrorCode.CONFLICT);
        Member saved = members.save(Member.create(tenantId, command.userId(), command.memberNumber(), command.fullName(), command.phone()));
        audit.record(new AuditEvent("MEMBER_CREATED", "MEMBER", saved.getId(), AuditResult.SUCCESS, "member-service", Map.of()));
        return view(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberView find(CurrentActor actor, Long memberId) {
        Long tenantId = guard.requireTenant(actor);
        Member member = members.findByTenantIdAndId(tenantId, memberId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (actor.roles().contains(RoleCode.MEMBER) && !Objects.equals(actor.userId(), member.getUserId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!actor.roles().contains(RoleCode.MEMBER) && !actor.hasPermission("member:read")) throw new BusinessException(ErrorCode.FORBIDDEN);
        return view(member);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberView findSelf(CurrentActor actor) {
        Long tenantId = guard.requireTenant(actor);
        Member member = members.findByTenantIdAndUserId(tenantId, actor.userId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return view(member);
    }

    @Override
    @Transactional
    public void update(CurrentActor actor, Long memberId, String fullName, String phone) {
        requireAdmin(actor, "member:write");
        Member member = findTenant(actor, memberId);
        if (members.existsByTenantIdAndPhone(ActorAccess.tenantId(actor), phone) && !phone.equals(member.getPhone())) throw new BusinessException(ErrorCode.CONFLICT);
        member.update(fullName, phone);
        members.save(member);
        audit.record(new AuditEvent("MEMBER_UPDATED", "MEMBER", memberId, AuditResult.SUCCESS, "member-service", Map.of()));
    }

    @Override
    @Transactional
    public void suspend(CurrentActor actor, Long memberId) {
        requireAdmin(actor, "member:write");
        Member member = findTenant(actor, memberId);
        member.suspend();
        members.save(member);
        audit.record(new AuditEvent("MEMBER_SUSPENDED", "MEMBER", memberId, AuditResult.SUCCESS, "member-service", Map.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberView> list(CurrentActor actor, String query, Pageable pageable) {
        requireAdmin(actor, "member:read");
        Pageable effective = pageable == null ? Pageable.ofSize(20) : pageable;
        Long tenantId = ActorAccess.tenantId(actor);
        var page = (query == null || query.isBlank())
                ? members.findAllByTenantId(tenantId, effective)
                : members.searchByTenantId(tenantId, query.trim(), effective);
        return new PageResponse<>(page.map(DefaultMemberService::view).getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Member findTenant(CurrentActor actor, Long id) {
        return members.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static void requireAdmin(CurrentActor actor, String permission) {
        ActorAccess.requireGymAdmin(actor, permission);
    }
    private static MemberView view(Member member) { return new MemberView(member.getId(), member.getTenantId(), member.getUserId(), member.getMemberNumber(), member.getFullName(), member.getPhone(), member.getStatus()); }
}
