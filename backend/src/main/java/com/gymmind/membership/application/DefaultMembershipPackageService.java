package com.gymmind.membership.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.membership.domain.model.MembershipPackage;
import com.gymmind.membership.domain.repository.MembershipPackageRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DefaultMembershipPackageService implements MembershipPackageService {

    private final MembershipPackageRepository packages;
    private final AuditRecorder audit;

    public DefaultMembershipPackageService(MembershipPackageRepository packages, AuditRecorder audit) {
        this.packages = packages;
        this.audit = audit;
    }

    @Override
    @Transactional
    public MembershipPackageView create(CurrentActor actor, CreateMembershipPackageCommand command) {
        require(actor, "membership:write");
        Long tenantId = ActorAccess.tenantId(actor);
        if (command == null || command.tenantId() != null && !tenantId.equals(command.tenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        MembershipPackage saved = packages.save(MembershipPackage.create(
                tenantId, command.name(), command.entitlementType(), command.entitlementCount(),
                command.validDays(), command.price(), command.currency()));
        audit.record(new AuditEvent("MEMBERSHIP_PACKAGE_CREATED", "MEMBERSHIP_PACKAGE", saved.getId(),
                AuditResult.SUCCESS, "membership-service", Map.of()));
        return MembershipPackageView.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPackageView find(CurrentActor actor, Long id) {
        require(actor, "membership:read");
        MembershipPackage membershipPackage = packages.findByTenantIdAndId(ActorAccess.tenantId(actor), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return MembershipPackageView.from(membershipPackage);
    }

    private static void require(CurrentActor actor, String permission) {
        ActorAccess.requireGymAdmin(actor, permission);
    }
}
