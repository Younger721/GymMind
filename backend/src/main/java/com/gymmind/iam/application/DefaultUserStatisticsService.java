package com.gymmind.iam.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.member.domain.model.Member;
import com.gymmind.member.domain.repository.MemberRepository;
import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.model.MembershipPackage;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import com.gymmind.membership.domain.repository.MembershipPackageRepository;
import com.gymmind.membership.application.MembershipDisplay;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.Clock;

@Service
public class DefaultUserStatisticsService implements UserStatisticsService {
    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final MemberRepository members;
    private final MemberMembershipRepository memberships;
    private final MembershipPackageRepository packages;
    private final Clock clock;
    private final TenantRepository tenants;

    @Autowired
    public DefaultUserStatisticsService(UserAccountRepository users, UserRoleRepository userRoles,
                                        MemberRepository members, MemberMembershipRepository memberships,
                                        MembershipPackageRepository packages, TenantRepository tenants) {
        this.users = Objects.requireNonNull(users, "users");
        this.userRoles = Objects.requireNonNull(userRoles, "userRoles");
        this.members = Objects.requireNonNull(members, "members");
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.packages = Objects.requireNonNull(packages, "packages");
        this.clock = Clock.systemUTC();
        this.tenants = tenants;
    }

    public DefaultUserStatisticsService(UserAccountRepository users, UserRoleRepository userRoles,
                                        MemberRepository members, MemberMembershipRepository memberships,
                                        MembershipPackageRepository packages) {
        this(users, userRoles, members, memberships, packages, null);
    }

    /** Backward-compatible constructor for focused service tests. */
    public DefaultUserStatisticsService(UserAccountRepository users, UserRoleRepository userRoles) {
        this.users = Objects.requireNonNull(users, "users");
        this.userRoles = Objects.requireNonNull(userRoles, "userRoles");
        this.members = null;
        this.memberships = null;
        this.packages = null;
        this.clock = Clock.systemUTC();
        this.tenants = null;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserAdministrationService.UserSummary> list(CurrentActor actor, Pageable pageable) {
        return list(actor, pageable, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserAdministrationService.UserSummary> list(CurrentActor actor, Pageable pageable, Long requestedTenantId, String displayName) {
        List<UserAdministrationService.UserSummary> visible = visibleUsers(actor);
        if (requestedTenantId != null) {
            if (!ActorAccess.isSuperAdmin(actor) && !requestedTenantId.equals(ActorAccess.tenantId(actor))) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            visible = visible.stream().filter(user -> requestedTenantId.equals(user.tenantId())).toList();
        }
        if (displayName != null && !displayName.isBlank()) {
            String keyword = displayName.trim().toLowerCase(java.util.Locale.ROOT);
            visible = visible.stream().filter(user -> user.displayName() != null
                    && user.displayName().toLowerCase(java.util.Locale.ROOT).contains(keyword)).toList();
        }
        Pageable effective = pageable == null ? Pageable.ofSize(20) : pageable;
        int from = Math.min((int) effective.getOffset(), visible.size());
        int to = Math.min(from + effective.getPageSize(), visible.size());
        List<UserAdministrationService.UserSummary> content = visible.subList(from, to);
        int totalPages = effective.getPageSize() <= 0 ? 0
                : (int) Math.ceil((double) visible.size() / effective.getPageSize());
        return new PageResponse<>(content, effective.getPageNumber(), effective.getPageSize(), visible.size(), totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatisticsView statistics(CurrentActor actor) {
        List<UserAdministrationService.UserSummary> visible = visibleUsers(actor);
        long members = visible.stream().filter(value -> value.roles().contains(RoleCode.MEMBER)).count();
        long coaches = visible.stream().filter(value -> value.roles().contains(RoleCode.COACH)).count();
        long gymAdmins = visible.stream().filter(value -> value.roles().contains(RoleCode.GYM_ADMIN)).count();
        long platformAdmins = visible.stream().filter(value -> value.roles().contains(RoleCode.PLATFORM_ADMIN)).count();
        return new UserStatisticsView(visible.size(), members, coaches, gymAdmins, platformAdmins,
                membershipTypeCounts(visible));
    }

    private Map<String, Long> membershipTypeCounts(List<UserAdministrationService.UserSummary> visible) {
        if (this.members == null || this.memberships == null || this.packages == null) return Map.of();
        Map<String, Long> counts = new LinkedHashMap<>();
        visible.stream()
                .filter(summary -> summary.roles().contains(RoleCode.MEMBER) && summary.tenantId() != null)
                .forEach(summary -> {
                    Member member = members.findByTenantIdAndUserId(summary.tenantId(), summary.id()).orElse(null);
                    if (member == null) return;
                    MemberMembership membership = memberships
                            .findActiveByTenantIdAndMemberId(summary.tenantId(), member.getId()).orElse(null);
                    if (membership == null) return;
                    MembershipPackage packageValue = packages
                            .findByTenantIdAndId(summary.tenantId(), membership.getPackageId()).orElse(null);
                    String name = packageValue == null ? "其他套餐" : ProfileTypeMapper.type(packageValue.getName());
                    counts.merge(name, 1L, Long::sum);
                });
        return Map.copyOf(counts);
    }

    private static final class ProfileTypeMapper {
        private static String type(String packageName) {
            if (packageName == null || packageName.isBlank()) return "其他套餐";
            if (packageName.contains("年卡")) return "年卡";
            if (packageName.contains("月卡")) return "月卡";
            if (packageName.contains("周卡")) return "周卡";
            return packageName;
        }
    }

    private List<UserAdministrationService.UserSummary> visibleUsers(CurrentActor actor) {
        if (actor == null) throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        List<UserAccount> accounts;
        if (ActorAccess.isSuperAdmin(actor)) {
            accounts = users.findAll();
        } else {
            Long tenantId = ActorAccess.tenantId(actor);
            if (tenantId == null || !actor.roles().contains(RoleCode.GYM_ADMIN)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            accounts = users.findAllByTenantId(tenantId, Pageable.ofSize(1000)).getContent();
        }
        return accounts.stream()
                .map(this::summary)
                .filter(summary -> ActorAccess.isSuperAdmin(actor)
                        || summary.id().equals(actor.userId())
                        || summary.roles().contains(RoleCode.COACH)
                        || summary.roles().contains(RoleCode.MEMBER))
                .sorted(Comparator.comparing(UserAdministrationService.UserSummary::id))
                .toList();
    }

    private UserAdministrationService.UserSummary summary(UserAccount user) {
        Set<RoleCode> roles = userRoles.findRoleCodesByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            roles = user.getRoleCode() == null ? Set.of() : EnumSet.of(user.getRoleCode());
        }
        String membershipType = null;
        Integer membershipDaysRemaining = null;
        if (members != null && memberships != null && packages != null && roles.contains(RoleCode.MEMBER) && user.getTenantId() != null) {
            Member member = members.findByTenantIdAndUserId(user.getTenantId(), user.getId()).orElse(null);
            if (member != null) {
                MemberMembership membership = memberships.findActiveByTenantIdAndMemberId(user.getTenantId(), member.getId()).orElse(null);
                if (membership != null) {
                    MembershipPackage pkg = packages.findByTenantIdAndId(user.getTenantId(), membership.getPackageId()).orElse(null);
                    membershipType = MembershipDisplay.type(pkg == null ? null : pkg.getValidDays(), pkg == null ? null : pkg.getName());
                    membershipDaysRemaining = Math.toIntExact(MembershipDisplay.daysRemaining(membership.getExpiresAt(), clock));
                }
            }
        }
        String tenantName = tenants == null || user.getTenantId() == null ? null
                : tenants.findById(user.getTenantId()).map(t -> t.getName()).orElse(null);
        return new UserAdministrationService.UserSummary(user.getId(), user.getTenantId(), tenantName,
                user.getNormalizedUsername(), user.getNormalizedEmail(), user.getDisplayName(), user.getPhone(),
                user.getStatus(), Set.copyOf(roles), membershipType, membershipDaysRemaining);
    }
}
