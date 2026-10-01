package com.gymmind.workout.application;
import com.gymmind.shared.security.CurrentActor;
import java.time.LocalDate;
public interface TrainingOverviewService { TrainingOverviewView overview(CurrentActor actor, LocalDate since, LocalDate until, Long memberId); }
