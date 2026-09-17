package com.gymmind.tenancy.application;

import com.gymmind.shared.security.CurrentActor;

public interface PlatformStatsService {
    PlatformStatsView stats(CurrentActor actor);
}
