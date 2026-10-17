package com.gymmind.membership.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipChangeRequestTest {

    @Test
    void approvesFreezeRequest() {
        MembershipChangeRequest request = MembershipChangeRequest.createFreeze(
                11L, 100L, 41L, 7L,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25), "出差");

        request.approve(99L, "同意");

        assertThat(request.getStatus()).isEqualTo(MembershipChangeRequestStatus.APPROVED);
        assertThat(request.getReviewerId()).isEqualTo(99L);
    }

    @Test
    void rejectsSelfApproval() {
        MembershipChangeRequest request = MembershipChangeRequest.createTransfer(
                11L, 100L, 41L, 7L, 42L, "转给家人");

        assertThatThrownBy(() -> request.approve(7L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
