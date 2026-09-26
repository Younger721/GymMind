package com.gymmind.member.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTenantTest {
    @Test
    void sameMemberCanBelongToTwoTenants() {
        MemberTenant first = MemberTenant.create(7L, 11L, "A-001", "13800000001");
        MemberTenant second = MemberTenant.create(7L, 12L, "B-001", "13800000002");
        assertThat(first.getMemberId()).isEqualTo(second.getMemberId());
        assertThat(first.getTenantId()).isNotEqualTo(second.getTenantId());
    }

    @Test
    void membershipRequiresPositiveIdsAndContactFields() {
        assertThatThrownBy(() -> MemberTenant.create(0L, 11L, "A-001", "13800000001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberTenant.create(7L, 11L, "", "13800000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
