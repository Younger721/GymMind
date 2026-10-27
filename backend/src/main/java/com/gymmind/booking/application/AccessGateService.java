package com.gymmind.booking.application;

import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.booking.domain.model.GymCheckIn;
import com.gymmind.booking.domain.repository.GymCheckInRepository;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.model.MemberStatus;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 闸机名单与通行记录。人脸留在闸机，这里只处理会员编号和到店。 */
@Service
public class AccessGateService {

    private final MemberRepository members;
    private final MemberMembershipRepository memberships;
    private final GymCheckInRepository checkIns;
    private final AuditRecorder audit;
    private final Clock clock;

    public AccessGateService(MemberRepository members,
                              MemberMembershipRepository memberships,
                              GymCheckInRepository checkIns,
                              AuditRecorder audit,
                              Clock clock) {
        this.members = members;
        this.memberships = memberships;
        this.checkIns = checkIns;
        this.audit = audit;
        this.clock = clock;
    }

    /** 当前可通行会员。闸机用这份名单覆盖本地人员，不在名单里的人删除。 */
    @Transactional(readOnly = true)
    public List<AccessMember> whitelist(CurrentActor actor) {
        Long tenantId = requireTenant(actor);
        Instant now = clock.instant();
        List<AccessMember> allowed = new ArrayList<>();
        var page = members.findAllByTenantId(tenantId, PageRequest.of(0, 1000));
        for (Member member : page.getContent()) {
            if (canPass(tenantId, member, now)) {
                allowed.add(new AccessMember(member.getMemberNumber(), member.getFullName()));
            }
        }
        return allowed;
    }

    /** 闸机放行后上报。套餐无效拒绝进入；同一天重复通行不再记一次出勤。 */
    @Transactional
    public boolean recordPass(CurrentActor actor, String memberNumber, String gateId) {
        Long tenantId = requireTenant(actor);
        if (memberNumber == null || memberNumber.isBlank() || gateId == null || gateId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        Instant now = clock.instant();
        Member member = members.findByTenantIdAndMemberNumber(tenantId, memberNumber.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!canPass(tenantId, member, now)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        if (checkIns.countByTenantIdAndMemberIdAndCheckInAtBetween(tenantId, member.getId(), start, end) > 0) {
            return false;
        }
        checkIns.save(GymCheckIn.create(tenantId, member.getId(), now, actor.userId(), null));
        audit.record(new AuditEvent("CHECKIN_RECORDED", "CHECKIN", member.getId(),
                AuditResult.SUCCESS, "access-gate", Map.of("resourceName", gateId.trim())));
        return true;
    }

    private static Long requireTenant(CurrentActor actor) {
        ActorAccess.requireGymAdmin(actor, "checkin:write");
        return ActorAccess.requireTenantId(actor);
    }

    private boolean canPass(Long tenantId, Member member, Instant now) {
        if (member.getStatus() != MemberStatus.ACTIVE
                || member.getMemberNumber() == null
                || member.getMemberNumber().isBlank()) {
            return false;
        }
        return memberships.findActiveByTenantIdAndMemberId(tenantId, member.getId())
                .filter(membership -> membership.isUsableAt(now))
                .isPresent();
    }

    public record AccessMember(String memberNumber, String name) {}
}
