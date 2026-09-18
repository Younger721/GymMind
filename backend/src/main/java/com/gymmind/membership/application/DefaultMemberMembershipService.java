package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MembershipPackage;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.membership.domain.repository.MembershipPackageRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DefaultMemberMembershipService implements MemberMembershipService {

    private final MemberMembershipRepository memberships;
    private final MembershipPackageRepository packages;
    private final AuditRecorder audit;

    public DefaultMemberMembershipService(
            MemberMembershipRepository memberships,
            MembershipPackageRepository packages,
            AuditRecorder audit) {
        this.memberships = memberships;
        this.packages = packages;
        this.audit = audit;
    }

    @Override
    @Transactional
    public MemberMembershipView grant(CurrentActor actor, GrantMembershipCommand command) {
        ActorAccess.requireGymAdmin(actor, "membership:write");
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        MembershipPackage membershipPackage = packages.findByTenantIdAndId(tenantId, command.packageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        MemberMembership membership = memberships.save(MemberMembership.create(
                tenantId, command.memberId(), command.packageId(), membershipPackage.getEntitlementCount(),
                command.startsAt(), command.expiresAt()));
        audit.record(new AuditEvent("MEMBERSHIP_GRANTED", "MEMBERSHIP", membership.getId(),
                AuditResult.SUCCESS, "membership-service", Map.of()));
        return MemberMembershipView.from(membership);
    }
}
