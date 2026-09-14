package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.ChurnRiskResult;
import com.gymmind.analytics.domain.ChurnSignals;
import com.gymmind.shared.security.CurrentActor;
public interface ChurnRiskService { ChurnRiskResult assess(CurrentActor actor, ChurnSignals signals); }
