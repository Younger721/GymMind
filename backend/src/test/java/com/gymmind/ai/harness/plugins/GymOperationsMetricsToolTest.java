package com.gymmind.ai.harness.plugins;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void parsesOnlyWhitelistedStructuredMetrics() throws Exception {
        var mapper = new ObjectMapper();
        var args = mapper.readTree("{\"metrics\":[\"UNIQUE_VISITORS\",\"ATTENDANCE_RATE\"],\"period\":\"PREVIOUS_MONTH\",\"groupBy\":\"WEEK\",\"comparePrevious\":true}");

        var intent = GymOperationsMetricsTool.parseStructuredIntent(args, LocalDate.of(2026, 10, 2));

        assertThat(intent.metrics()).containsExactly(
                OperationsMetric.UNIQUE_VISITORS, OperationsMetric.ATTENDANCE_RATE);
        assertThat(intent.query().from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(intent.query().to()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(intent.groupBy()).isEqualTo(OperationsGroupBy.WEEK);
        assertThat(intent.comparePrevious()).isTrue();
    }

    @Test
    void rejectsUnknownMetricAndArbitrarySqlArguments() throws Exception {
        var mapper = new ObjectMapper();

        assertThatThrownBy(() -> GymOperationsMetricsTool.parseStructuredIntent(
                mapper.readTree("{\"metrics\":[\"DROP_TABLE\"]}"), LocalDate.of(2026, 10, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的运营指标");
        assertThatThrownBy(() -> GymOperationsMetricsTool.parseStructuredIntent(
                mapper.readTree("{\"sql\":\"SELECT 1\"}"), LocalDate.of(2026, 10, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不接受 SQL");
    }

    @Test
    void exposesSafeStructuredCapabilitySchema() {
        var descriptor = new GymOperationsMetricsCapability((GymOperationsMetricsTool) null).descriptor();

        assertThat(descriptor.parameterSchema().path("properties").path("metrics").path("items").path("enum").toString())
                .contains("UNIQUE_VISITORS");
        assertThat(descriptor.parameterSchema().path("properties").path("groupBy").path("enum").toString())
                .contains("WEEK");
    }
}
