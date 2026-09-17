package com.gymmind.ai.harness.domain;

import java.util.Optional;

public interface PlatformHarnessConfigRepository {
    PlatformHarnessConfig save(PlatformHarnessConfig config);

    Optional<PlatformHarnessConfig> findSingleton();
}
