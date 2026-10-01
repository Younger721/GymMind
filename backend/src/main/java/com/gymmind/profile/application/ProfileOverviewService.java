package com.gymmind.profile.application;

import com.gymmind.shared.security.CurrentActor;

import java.time.LocalDate;

public interface ProfileOverviewService {
    ProfileOverview overview(CurrentActor actor, LocalDate from, LocalDate to);
}
