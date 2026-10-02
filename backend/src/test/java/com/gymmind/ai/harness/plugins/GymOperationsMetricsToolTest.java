package com.gymmind.ai.harness.plugins;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GymOperationsMetricsToolTest {
    @Test
    void recognizesGymOperationsQuestions() {
        assertThat(GymOperationsMetricsTool.matchesQuestion("上个月来了多少会员？")).isTrue();
        assertThat(GymOperationsMetricsTool.matchesQuestion("和上上个月相比变化多少？")).isTrue();
        assertThat(GymOperationsMetricsTool.matchesQuestion("最近预约到店率怎么样")).isTrue();
        assertThat(GymOperationsMetricsTool.matchesQuestion("如何做高位下拉")).isFalse();
    }

    @Test
    void resolvesPreviousCalendarMonthFromReferenceDate() {
        var range = GymOperationsMetricsTool.previousCalendarMonth(LocalDate.of(2026, 10, 2));

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(range.periodLabel()).isEqualTo("2026年9月");
    }

    @Test
    void calculatesVisitorChangeRateWithoutDivisionByZero() {
        assertThat(GymOperationsMetricsTool.changeRate(186, 214)).isEqualByComparingTo("-13.1");
        assertThat(GymOperationsMetricsTool.changeRate(20, 0)).isEqualByComparingTo("100.0");
    }

    @Test
    void comparesLastMonthWithTheMonthBeforeIt() throws Exception {
        var method = GymOperationsMetricsTool.class.getDeclaredMethod("resolveQuery", String.class, LocalDate.class);
        method.setAccessible(true);

        var tool = new GymOperationsMetricsTool(null);
        var range = (OperationsMetricsQuery) method.invoke(tool, "和上上个月相比变化多少？", LocalDate.of(2026, 10, 2));

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 9, 30));
    }
}
