package com.gymmind.analytics.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChurnRiskCalculatorTest {
    private final ChurnRiskCalculator calculator = new ChurnRiskCalculator();

    @Test
    void combinesExplainableSignalsIntoHighRisk() {
        var result = calculator.calculate(new ChurnSignals(45, 1, 8, 0, 0, 3, true));
        assertThat(result.score()).isGreaterThanOrEqualTo(70);
        assertThat(result.level()).isEqualTo(ChurnRiskLevel.HIGH);
        assertThat(result.reasons()).contains("长期未到店", "到店频次下降", "会籍即将到期");
    }

    @Test
    void healthyRecentActivityIsLowRisk() {
        var result = calculator.calculate(new ChurnSignals(3, 6, 5, 2, 2, 20, false));
        assertThat(result.score()).isLessThan(30);
        assertThat(result.level()).isEqualTo(ChurnRiskLevel.LOW);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void flagsBookingsWithoutCompletion() {
        var result = calculator.calculate(new ChurnSignals(5, 4, 4, 3, 0, 60, false));
        assertThat(result.reasons()).contains("有预约但未完课");
    }

    @Test
    void clampsInvalidSignalsAndKeepsScoreBounded() {
        var result = calculator.calculate(new ChurnSignals(-1, -2, 100, -4, -1, -4, true));
        assertThat(result.score()).isBetween(0, 100);
    }
}
