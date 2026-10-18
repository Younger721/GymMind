package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.member.domain.repository.MemberTenantRepository;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MembershipChangeRequest;
import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;
import com.gymmind.membership.domain.model.MembershipChangeType;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.membership.domain.repository.MembershipChangeRequestRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DefaultMembershipChangeRequestService implements MembershipChangeRequestService {

    private static final int MAX_FREEZE_DAYS = 30;

    private final MembershipChangeRequestRepository requests;
    private final MemberMembershipRepository memberships;
    private final MemberRepository members;
    private final MemberTenantRepository memberTenants;
    private final AuditRecorder audit;
    private final Clock clock;

    public DefaultMembershipChangeRequestService(MembershipChangeRequestRepository requests,
                                                 MemberMembershipRepository memberships,
                                                 MemberRepository members,
                                                 MemberTenantRepository memberTenants,
                                                 AuditRecorder audit,
                                                 Clock clock) {
        this.requests = requests;
        this.memberships = memberships;
        this.members = members;
        this.memberTenants = memberTenants;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MembershipChangeRequestView create(CurrentActor actor, CreateMembershipChangeCommand command) {
        if (ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
        requireRequestPermission(actor);
        if (command == null || command.type() == null || command.membershipId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (actor.roles().contains(RoleCode.COACH)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long tenantId = ActorAccess.requireTenantId(actor);
        MemberMembership membership = loadUsableMembership(tenantId, command.membershipId());
        assertNoPendingRequest(tenantId, command.membershipId());
        ensureCanActOnMembership(actor, tenantId, membership.getMemberId());

        MembershipChangeRequest request;
        try {
            if (command.type() == MembershipChangeType.FREEZE) {
                LocalDate start = command.freezeStart();
                LocalDate end = command.freezeEnd();
                validateFreezePeriod(start, end);
                request = MembershipChangeRequest.createFreeze(
                        tenantId, command.membershipId(), membership.getMemberId(), actor.userId(),
                        start, end, command.reason());
            } else if (command.type() == MembershipChangeType.TRANSFER) {
                Long targetMemberId = command.targetMemberId();
                ensureMemberInTenant(tenantId, targetMemberId);
                request = MembershipChangeRequest.createTransfer(
                        tenantId, command.membershipId(), membership.getMemberId(), actor.userId(),
                        targetMemberId, command.reason());
            } else {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        MembershipChangeRequest saved = requests.save(request);
        audit.record(new AuditEvent("MEMBERSHIP_CHANGE_REQUESTED", "MEMBERSHIP_CHANGE", saved.getId(),
                AuditResult.SUCCESS, "membership-change-service",
                Map.of("source", saved.getType().name())));
        return MembershipChangeRequestView.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipChangeRequestView> list(CurrentActor actor, MembershipChangeRequestStatus status) {
        requireReadPermission(actor);
        Long tenantId = ActorAccess.requireTenantId(actor);
        List<MembershipChangeRequest> rows;
        if (isTenantAdminViewer(actor)) {
            rows = status == null
                    ? requests.findAllByTenantIdOrderByIdDesc(tenantId)
                    : requests.findAllByTenantIdAndStatusOrderByIdDesc(tenantId, status);
        } else {
            Long memberId = resolveMemberId(actor, tenantId);
            rows = requests.findAllByTenantIdAndMemberIdOrderByIdDesc(tenantId, memberId);
            if (status != null) {
                rows = rows.stream().filter(row -> row.getStatus() == status).toList();
            }
        }
        return rows.stream().map(MembershipChangeRequestView::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipChangeRequestView find(CurrentActor actor, Long id) {
        requireReadPermission(actor);
        Long tenantId = ActorAccess.requireTenantId(actor);
        MembershipChangeRequest request = loadRequest(tenantId, id);
        ensureCanView(actor, tenantId, request);
        return MembershipChangeRequestView.from(request);
    }

    @Override
    @Transactional
    public MembershipChangeRequestView cancel(CurrentActor actor, Long id) {
        if (ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
        requireCancelPermission(actor);
        Long tenantId = ActorAccess.requireTenantId(actor);
        MembershipChangeRequest request = loadRequest(tenantId, id);
        ensureCanCancel(actor, request);
        try {
            request.cancel();
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        MembershipChangeRequest saved = requests.save(request);
        audit.record(new AuditEvent("MEMBERSHIP_CHANGE_CANCELLED", "MEMBERSHIP_CHANGE", saved.getId(),
                AuditResult.SUCCESS, "membership-change-service", Map.of("source", "cancel")));
        return MembershipChangeRequestView.from(saved);
    }

    @Override
    @Transactional
    public MembershipChangeRequestView approve(CurrentActor actor, Long id, String comment) {
        ActorAccess.requireGymAdminApproval(actor, "membership:change:approve");
        Long tenantId = ActorAccess.requireTenantId(actor);
        MembershipChangeRequest request = loadRequest(tenantId, id);
        MemberMembership membership = memberships.findByTenantIdAndId(tenantId, request.getMembershipId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        try {
            request.approve(actor.userId(), comment);
            if (request.getType() == MembershipChangeType.FREEZE) {
                LocalDate today = LocalDate.now(clock);
                membership.applyApprovedFreeze(request.getFreezeStart(), request.getFreezeEnd(), today);
                memberships.save(membership);
            } else {
                ensureMemberInTenant(tenantId, request.getTargetMemberId());
                membership.transferTo(request.getTargetMemberId());
                memberships.save(membership);
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        MembershipChangeRequest saved = requests.save(request);
        audit.record(new AuditEvent("MEMBERSHIP_CHANGE_APPROVED", "MEMBERSHIP_CHANGE", saved.getId(),
                AuditResult.SUCCESS, "membership-change-service",
                Map.of("source", saved.getType().name())));
        return MembershipChangeRequestView.from(saved);
    }

    @Override
    @Transactional
    public MembershipChangeRequestView reject(CurrentActor actor, Long id, String comment) {
        ActorAccess.requireGymAdminApproval(actor, "membership:change:approve");
        Long tenantId = ActorAccess.requireTenantId(actor);
        MembershipChangeRequest request = loadRequest(tenantId, id);
        try {
            request.reject(actor.userId(), comment);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        MembershipChangeRequest saved = requests.save(request);
        audit.record(new AuditEvent("MEMBERSHIP_CHANGE_REJECTED", "MEMBERSHIP_CHANGE", saved.getId(),
                AuditResult.SUCCESS, "membership-change-service", Map.of("source", "reject")));
        return MembershipChangeRequestView.from(saved);
    }

    private void requireRequestPermission(CurrentActor actor) {
        if (actor == null || !actor.hasPermission("membership:change:request")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireReadPermission(CurrentActor actor) {
        if (actor == null || !actor.hasPermission("membership:change:read")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireCancelPermission(CurrentActor actor) {
        if (actor == null || !actor.hasPermission("membership:change:cancel")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean isGymAdmin(CurrentActor actor) {
        return actor != null && actor.roles().contains(RoleCode.GYM_ADMIN);
    }

    /**
     * 租户内管理视角：健身房管理员，以及只读巡检的平台管理员。
     * 平台管理员不参与审批（见 {@link com.gymmind.shared.security.ActorAccess#requireGymAdminApproval}），
     * 但可以查看所选门店的申请列表与详情，避免进入页面时被当成会员解析而报 404。
     */
    private boolean isTenantAdminViewer(CurrentActor actor) {
        return isGymAdmin(actor) || ActorAccess.isSuperAdmin(actor);
    }

    private MemberMembership loadUsableMembership(Long tenantId, Long membershipId) {
        MemberMembership membership = memberships.findByTenantIdAndId(tenantId, membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!membership.isUsableAt(clock.instant())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return membership;
    }

    private void assertNoPendingRequest(Long tenantId, Long membershipId) {
        if (requests.existsByTenantIdAndMembershipIdAndStatus(
                tenantId, membershipId, MembershipChangeRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private void validateFreezePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        LocalDate today = LocalDate.now(clock);
        if (end.isBefore(today)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_FREEZE_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private void ensureMemberInTenant(Long tenantId, Long memberId) {
        if (memberId == null || !memberTenants.existsByTenantIdAndMemberId(tenantId, memberId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private void ensureCanActOnMembership(CurrentActor actor, Long tenantId, Long memberId) {
        if (isGymAdmin(actor)) {
            return;
        }
        if (!actor.roles().contains(RoleCode.MEMBER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long ownMemberId = resolveMemberId(actor, tenantId);
        if (!Objects.equals(ownMemberId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void ensureCanView(CurrentActor actor, Long tenantId, MembershipChangeRequest request) {
        if (isTenantAdminViewer(actor)) {
            return;
        }
        Long ownMemberId = resolveMemberId(actor, tenantId);
        if (!Objects.equals(ownMemberId, request.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void ensureCanCancel(CurrentActor actor, MembershipChangeRequest request) {
        if (isGymAdmin(actor)) {
            return;
        }
        if (!Objects.equals(actor.userId(), request.getApplicantUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Long resolveMemberId(CurrentActor actor, Long tenantId) {
        return members.findByTenantIdAndUserId(tenantId, actor.userId())
                .map(com.gymmind.member.domain.model.Member::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private MembershipChangeRequest loadRequest(Long tenantId, Long id) {
        return requests.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
