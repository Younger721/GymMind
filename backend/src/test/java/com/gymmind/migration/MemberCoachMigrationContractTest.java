package com.gymmind.migration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MemberCoachMigrationContractTest {
    @Test
    void migrationBackfillsMembershipsAndInvalidatesCrossTenantRelations() throws Exception {
        String sql = Files.readString(Path.of("..", "docs", "database", "2026-09-26-member-coach-relationship-migration.sql"), StandardCharsets.UTF_8);
        assertThat(sql).contains("INSERT INTO member_tenant")
                .contains("mt.member_id = a.member_id")
                .contains("mt.tenant_id = a.tenant_id")
                .contains("SET a.status = 'ENDED'");
    }

    @Test
    void migrationProtectsMembershipAndRelationUniqueness() throws Exception {
        String sql = Files.readString(Path.of("..", "docs", "database", "2026-09-26-member-coach-relationship-migration.sql"), StandardCharsets.UTF_8);
        assertThat(sql).contains("uk_member_tenant_member_gym")
                .contains("idx_coach_assignment_tenant_status")
                .contains("NOT EXISTS");
    }
}
