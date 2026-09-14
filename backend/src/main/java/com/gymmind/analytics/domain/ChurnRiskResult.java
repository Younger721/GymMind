package com.gymmind.analytics.domain;
import java.util.List;
public record ChurnRiskResult(int score, ChurnRiskLevel level, List<String> reasons) { }
