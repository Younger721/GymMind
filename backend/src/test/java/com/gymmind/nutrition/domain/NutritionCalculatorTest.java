package com.gymmind.nutrition.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NutritionCalculatorTest {
    private final NutritionCalculator calculator = new DefaultNutritionCalculator();

    @Test
    void calculatesBmiWithMetricInputs() {
        assertThat(calculator.calculateBmi(80, 180)).isEqualTo(24.6914, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void calculatesMifflinStJeorBmrAndTdee() {
        assertThat(calculator.calculateBmr(Sex.MALE, 80, 180, 30)).isEqualTo(1780.0);
        assertThat(calculator.calculateTdee(1780, 1.55)).isEqualTo(2759.0);
    }

    @Test
    void calculatesMacroGramsFromCalorieTarget() {
        MacroTargets macros = calculator.calculateMacros(2000, 0.30, 0.40, 0.30);
        assertThat(macros.proteinGrams()).isEqualTo(150.0);
        assertThat(macros.carbohydrateGrams()).isEqualTo(200.0);
        assertThat(macros.fatGrams()).isEqualTo(66.6667, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void rejectsInvalidPhysiologyAndRatios() {
        assertThatThrownBy(() -> calculator.calculateBmi(0, 180)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateMacros(2000, 0.5, 0.4, 0.2)).isInstanceOf(IllegalArgumentException.class);
    }
}
