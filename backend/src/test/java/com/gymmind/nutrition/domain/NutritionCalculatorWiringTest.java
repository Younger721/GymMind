package com.gymmind.nutrition.domain;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

class NutritionCalculatorWiringTest {
    @Test
    void defaultCalculatorIsRegisteredAsAStatelessComponent() {
        assertThat(DefaultNutritionCalculator.class.isAnnotationPresent(Component.class)).isTrue();
    }
}
