package com.gymmind.coach.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.coach.domain.model.CoachAssignment;
import com.gymmind.coach.domain.model.CoachAssignmentStatus;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Objects;

@Service
public class DefaultCoachAssignmentService implements CoachAssignmentService {
    private final CoachRepository coaches; private final CoachAssignmentRepository assignments; private final AuditRecorder audit;
    public DefaultCoachAssignmentService(CoachRepository coaches, CoachAssignmentRepository assignments, AuditRecorder audit) { this.coaches = coaches; this.assignments = assignments; this.audit = audit; }
    @Override @Transactional public void assign(CurrentActor actor, AssignCoachCommand command) {
        requireAdmin(actor); if (command == null || command.coachId() == null || command.memberId() == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        Long tenantId = ActorAccess.tenantId(actor);
        if (command.tenantId() != null && !tenantId.equals(command.tenantId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        coaches.findByTenantIdAndId(tenantId, command.coachId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (assignments.findActiveByTenantIdAndCoachIdAndMemberId(tenantId, command.coachId(), command.memberId()).isPresent()) return;
        assignments.save(CoachAssignment.create(tenantId, command.coachId(), command.memberId()));
        audit.record(new AuditEvent("COACH_ASSIGNED", "COACH_MEMBER_ASSIGNMENT", command.coachId(), AuditResult.SUCCESS, "coach-assignment-service", Map.of()));
    }
    @Override @Transactional public void unassign(CurrentActor actor, Long coachId, Long memberId) { requireAdmin(actor); assignments.deleteActiveByTenantIdAndCoachIdAndMemberId(ActorAccess.tenantId(actor), coachId, memberId); audit.record(new AuditEvent("COACH_UNASSIGNED", "COACH_MEMBER_ASSIGNMENT", coachId, AuditResult.SUCCESS, "coach-assignment-service", Map.of())); }
    @Override @Transactional public void request(CurrentActor actor, Long coachId) {
        if (actor == null || !actor.roles().contains(RoleCode.MEMBER) || actor.tenantId() == null || coachId == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        Long tenantId = actor.tenantId();
        coaches.findByTenantIdAndId(tenantId, coachId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Long memberId = actor.userId();
        if (assignments.existsByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coachId, memberId, CoachAssignmentStatus.PENDING)
                || assignments.existsByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coachId, memberId, CoachAssignmentStatus.ACTIVE)) return;
        CoachAssignment relation = CoachAssignment.createRequest(tenantId, coachId, memberId, actor.userId()); assignments.save(relation);
        audit.record(new AuditEvent("COACH_REQUESTED", "COACH_MEMBER_ASSIGNMENT", coachId, AuditResult.SUCCESS, "coach-assignment-service", Map.of()));
    }
    @Override @Transactional public void accept(CurrentActor actor, Long relationId) { transition(actor, relationId, CoachAssignmentStatus.PENDING, true, false); }
    @Override @Transactional public void reject(CurrentActor actor, Long relationId) { transition(actor, relationId, CoachAssignmentStatus.PENDING, false, false); }
    @Override @Transactional public void cancel(CurrentActor actor, Long relationId) { transition(actor, relationId, CoachAssignmentStatus.PENDING, false, true); }
    @Override @Transactional public void end(CurrentActor actor, Long relationId) { transition(actor, relationId, CoachAssignmentStatus.ACTIVE, false, true); }
    private void transition(CurrentActor actor, Long relationId, CoachAssignmentStatus expected, boolean accept, boolean memberOrEnd) {
        Long tenantId = ActorAccess.tenantId(actor); if (tenantId == null || relationId == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        CoachAssignment relation = assignments.findByIdAndTenantId(relationId, tenantId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        boolean coachOwner = actor.roles().contains(RoleCode.COACH) && coaches.findByTenantIdAndUserId(tenantId, actor.userId()).map(c -> Objects.equals(c.getId(), relation.getCoachId())).orElse(false);
        boolean memberOwner = actor.roles().contains(RoleCode.MEMBER) && Objects.equals(actor.userId(), relation.getMemberId());
        if ((accept || (!memberOrEnd && !memberOwner)) && !coachOwner) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (memberOrEnd && !memberOwner && !coachOwner && !ActorAccess.canManageTenant(actor)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (relation.getStatus() != expected) throw new BusinessException(ErrorCode.CONFLICT);
        if (accept) relation.accept(); else if (expected == CoachAssignmentStatus.PENDING && coachOwner) relation.reject(); else if (expected == CoachAssignmentStatus.PENDING) relation.cancel(); else relation.end();
        assignments.save(relation);
        audit.record(new AuditEvent("COACH_RELATION_UPDATED", "COACH_MEMBER_ASSIGNMENT", relation.getId(), AuditResult.SUCCESS, "coach-assignment-service", Map.of()));
    }
    @Override @Transactional(readOnly = true) public boolean canAccess(CurrentActor actor, Long memberId) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (tenantId == null || memberId == null) return false;
        if (ActorAccess.canManageTenant(actor)) return actor.hasPermission("member:read");
        if (!actor.roles().contains(RoleCode.COACH)) return false;
        return coaches.findByTenantIdAndUserId(tenantId, actor.userId()).map(coach -> assignments.existsByTenantIdAndCoachIdAndMemberIdAndStatus(tenantId, coach.getId(), memberId, CoachAssignmentStatus.ACTIVE)).orElse(false);
    }
    private static void requireAdmin(CurrentActor actor) { ActorAccess.requireGymAdmin(actor, "coach:assign"); }
}
