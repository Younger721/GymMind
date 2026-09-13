package com.gymmind.coach.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.coach.domain.model.CoachAssignment;
import com.gymmind.coach.domain.repository.CoachAssignmentRepository;
import com.gymmind.coach.domain.repository.CoachRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
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
        if (command.tenantId() != null && !actor.tenantId().equals(command.tenantId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        coaches.findByTenantIdAndId(actor.tenantId(), command.coachId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (assignments.findActiveByTenantIdAndCoachIdAndMemberId(actor.tenantId(), command.coachId(), command.memberId()).isPresent()) return;
        assignments.save(CoachAssignment.create(actor.tenantId(), command.coachId(), command.memberId()));
        audit.record(new AuditEvent("COACH_ASSIGNED", "COACH_MEMBER_ASSIGNMENT", command.coachId(), AuditResult.SUCCESS, "coach-assignment-service", Map.of()));
    }
    @Override @Transactional public void unassign(CurrentActor actor, Long coachId, Long memberId) { requireAdmin(actor); assignments.deleteActiveByTenantIdAndCoachIdAndMemberId(actor.tenantId(), coachId, memberId); audit.record(new AuditEvent("COACH_UNASSIGNED", "COACH_MEMBER_ASSIGNMENT", coachId, AuditResult.SUCCESS, "coach-assignment-service", Map.of())); }
    @Override @Transactional(readOnly = true) public boolean canAccess(CurrentActor actor, Long memberId) {
        if (actor == null || actor.tenantId() == null || memberId == null) return false;
        if (actor.roles().contains(RoleCode.GYM_ADMIN)) return actor.hasPermission("member:read");
        if (!actor.roles().contains(RoleCode.COACH)) return false;
        return coaches.findByTenantIdAndUserId(actor.tenantId(), actor.userId()).map(coach -> assignments.existsByTenantIdAndCoachIdAndMemberIdAndActive(actor.tenantId(), coach.getId(), memberId)).orElse(false);
    }
    private static void requireAdmin(CurrentActor actor) { if (actor == null || actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN) || !actor.hasPermission("coach:assign")) throw new BusinessException(ErrorCode.FORBIDDEN); }
}
