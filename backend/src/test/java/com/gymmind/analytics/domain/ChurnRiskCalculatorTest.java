package com.gymmind.analytics.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChurnRiskCalculatorTest {
    private final ChurnRiskCalculator calculator = new ChurnRiskCalculator();

    @Test void combinesExplainableSignalsIntoHighRisk() {
        var result = calculator.calculate(new ChurnSignals(45, 1, 8, 3, true));
        assertThat(result.score()).isGreaterThanOrEqualTo(70);
        assertThat(result.level()).isEqualTo(ChurnRiskLevel.HIGH);
        assertThat(result.reasons()).contains("长期未训练", "训练频率下降", "会员即将到期");
    }

    @Test void healthyRecentActivityIsLowRisk() {
        var result = calculator.calculate(new ChurnSignals(3, 4, 4, 20, false));
        assertThat(result.score()).isLessThan(30);
        assertThat(result.level()).isEqualTo(ChurnRiskLevel.LOW);
        assertThat(result.reasons()).isEmpty();
    }

    @Test void clampsInvalidSignalsAndKeepsScoreBounded() {
        var result = calculator.calculate(new ChurnSignals(-1, -2, 100, -4, true));
        assertThat(result.score()).isBetween(0, 100);
    }
}
