package com.gymmind.profile.application;

import com.gymmind.booking.domain.repository.GymCheckInRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DefaultProfileOverviewService implements ProfileOverviewService {
    private final UserAccountRepository users;
    private final MemberRepository members;
    private final GymCheckInRepository checkIns;
    private final MemberMembershipRepository memberships;
    private final MembershipPackageRepository packages;

    public DefaultProfileOverviewService(UserAccountRepository users,
                                         MemberRepository members,
                                         GymCheckInRepository checkIns,
                                         MemberMembershipRepository memberships,
                                         MembershipPackageRepository packages) {
        this.users = Objects.requireNonNull(users, "users");
        this.members = Objects.requireNonNull(members, "members");
        this.checkIns = Objects.requireNonNull(checkIns, "checkIns");
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.packages = Objects.requireNonNull(packages, "packages");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileOverview overview(CurrentActor actor, LocalDate from, LocalDate to) {
        if (actor == null) throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        LocalDate effectiveFrom = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate effectiveTo = to == null ? effectiveFrom.withDayOfMonth(effectiveFrom.lengthOfMonth()) : to;
        if (effectiveTo.isBefore(effectiveFrom)) throw new BusinessException(ErrorCode.VALIDATION_FAILED);

        UserAccount user = users.findById(actor.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Long tenantId = ActorAccess.tenantId(actor);
        Member member = actor.roles().contains(RoleCode.MEMBER) && tenantId != null
                ? members.findByTenantIdAndUserId(tenantId, actor.userId()).orElse(null)
                : null;
        if (member == null) {
            return new ProfileOverview(user.getId(), user.getNormalizedUsername(), user.getNormalizedEmail(),
                    user.getDisplayName(), user.getStatus(), effectiveFrom, effectiveTo, 0L, List.of(), null, null);
        }

        Instant start = effectiveFrom.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = effectiveTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long count = checkIns.countByTenantIdAndMemberIdAndCheckInAtBetween(tenantId, member.getId(), start, end);
        List<LocalDate> checkInDates = checkIns.findByTenantIdAndMemberIdAndCheckInAtBetween(tenantId, member.getId(), start, end)
                .stream()
                .map(value -> value.getCheckInAt().atZone(ZoneId.systemDefault()).toLocalDate())
                .distinct()
                .sorted()
                .toList();
        ProfileOverview.MembershipInfo membership = memberships
                .findActiveByTenantIdAndMemberId(tenantId, member.getId())
                .map(value -> membershipInfo(tenantId, value))
                .orElse(null);
        return new ProfileOverview(user.getId(), user.getNormalizedUsername(), user.getNormalizedEmail(),
                user.getDisplayName(), user.getStatus(), effectiveFrom, effectiveTo, count, checkInDates,
                new ProfileOverview.MemberInfo(member.getId(), member.getFullName(), member.getMemberNumber(),
                        member.getStatus(), member.getCheckInToken()), membership);
    }

    private ProfileOverview.MembershipInfo membershipInfo(Long tenantId, MemberMembership membership) {
        MembershipPackage packageValue = packages.findByTenantIdAndId(tenantId, membership.getPackageId()).orElse(null);
        String packageName = packageValue == null ? "未命名套餐" : packageValue.getName();
        return new ProfileOverview.MembershipInfo(membership.getId(), packageName, membershipType(packageName),
                membership.getStartsAt(), membership.getExpiresAt(), membership.getStatus());
    }

    static String membershipType(String packageName) {
        String normalized = packageName == null ? "" : packageName.toLowerCase(Locale.ROOT);
        if (normalized.contains("年卡")) return "年卡";
        if (normalized.contains("月卡")) return "月卡";
        if (normalized.contains("周卡")) return "周卡";
        return packageName == null || packageName.isBlank() ? "其他套餐" : packageName;
    }
}
