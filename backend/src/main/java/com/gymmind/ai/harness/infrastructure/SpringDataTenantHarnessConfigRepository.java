package com.gymmind.ai.harness.infrastructure;

import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTenantHarnessConfigRepository extends JpaRepository<TenantHarnessConfig, Long> {
}
