package com.gymmind.ai.harness.infrastructure;

import com.gymmind.ai.harness.domain.PlatformHarnessConfig;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPlatformHarnessConfigRepository extends JpaRepository<PlatformHarnessConfig, Long> {
}
