package com.gymmind.coach.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.coach.domain.model.Coach;
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
public class DefaultCoachService implements CoachService {
    private final CoachRepository coaches; private final AuditRecorder audit;
    public DefaultCoachService(CoachRepository coaches, AuditRecorder audit) { this.coaches = Objects.requireNonNull(coaches); this.audit = Objects.requireNonNull(audit); }
    @Override @Transactional public CoachView create(CurrentActor actor, CreateCoachCommand command) {
        requireAdmin(actor, "coach:write"); if (command == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        if (command.tenantId() != null && !actor.tenantId().equals(command.tenantId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        Coach saved = coaches.save(Coach.create(actor.tenantId(), command.userId(), command.coachNumber(), command.fullName(), command.phone()));
        audit.record(new AuditEvent("COACH_CREATED", "COACH", saved.getId(), AuditResult.SUCCESS, "coach-service", Map.of())); return view(saved);
    }
    @Override @Transactional(readOnly = true) public CoachView find(CurrentActor actor, Long coachId) {
        if (actor == null || actor.tenantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN);
        Coach coach = coaches.findByTenantIdAndId(actor.tenantId(), coachId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (actor.roles().contains(RoleCode.COACH) && !Objects.equals(actor.userId(), coach.getUserId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (actor.roles().contains(RoleCode.GYM_ADMIN) && !actor.hasPermission("coach:read")) throw new BusinessException(ErrorCode.FORBIDDEN);
        return view(coach);
    }
    @Override @Transactional public void update(CurrentActor actor, Long coachId, String fullName, String phone) { requireAdmin(actor, "coach:write"); Coach coach = findTenant(actor, coachId); coach.update(fullName, phone); coaches.save(coach); audit.record(new AuditEvent("COACH_UPDATED", "COACH", coachId, AuditResult.SUCCESS, "coach-service", Map.of())); }
    @Override @Transactional public void suspend(CurrentActor actor, Long coachId) { requireAdmin(actor, "coach:write"); Coach coach = findTenant(actor, coachId); coach.suspend(); coaches.save(coach); audit.record(new AuditEvent("COACH_SUSPENDED", "COACH", coachId, AuditResult.SUCCESS, "coach-service", Map.of())); }
    private Coach findTenant(CurrentActor actor, Long id) { return coaches.findByTenantIdAndId(actor.tenantId(), id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private static void requireAdmin(CurrentActor actor, String permission) { if (actor == null || actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN) || !actor.hasPermission(permission)) throw new BusinessException(ErrorCode.FORBIDDEN); }
    private static CoachView view(Coach c) { return new CoachView(c.getId(), c.getTenantId(), c.getUserId(), c.getCoachNumber(), c.getFullName(), c.getPhone(), c.getStatus()); }
}
