package com.gymmind.membership.application;

import com.gymmind.membership.domain.model.MemberMembership;
import com.gymmind.membership.domain.repository.MemberMembershipRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** 按停卡区间自动切换会籍 ACTIVE / SUSPENDED */
@Component
public class MembershipFreezeResumeScheduler {

    private final MemberMembershipRepository memberships;
    private final Clock clock;

    public MembershipFreezeResumeScheduler(MemberMembershipRepository memberships, Clock clock) {
        this.memberships = memberships;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${gymmind.membership.freeze-resume-delay-ms:3600000}")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now(clock);
        for (MemberMembership membership : memberships.findAllWithFreezePeriod()) {
            membership.refreshFreezeStatus(today);
            memberships.save(membership);
        }
    }
}
