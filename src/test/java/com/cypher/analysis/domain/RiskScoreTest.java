package com.cypher.analysis.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskScoreTest {

    @ParameterizedTest
    @CsvSource({
            "0.00, LOW",
            "0.19, LOW",
            "0.20, MEDIUM",
            "0.49, MEDIUM",
            "0.50, HIGH",
            "0.74, HIGH",
            "0.75, CRITICAL",
            "1.00, CRITICAL"
    })
    void mapsValueToRiskLevel(double value, RiskLevel expectedLevel) {
        RiskScore score = RiskScore.of(value);

        assertThat(score.level()).isEqualTo(expectedLevel);
        assertThat(score.isLow()).isEqualTo(expectedLevel == RiskLevel.LOW);
        assertThat(score.isMedium()).isEqualTo(expectedLevel == RiskLevel.MEDIUM);
        assertThat(score.isHigh()).isEqualTo(expectedLevel == RiskLevel.HIGH);
        assertThat(score.isCritical()).isEqualTo(expectedLevel == RiskLevel.CRITICAL);
        assertThat(score.blockAnticipation()).isEqualTo(expectedLevel == RiskLevel.CRITICAL);
        assertThat(score.requiresAttention()).isEqualTo(expectedLevel != RiskLevel.LOW);
    }

    @Test
    void ofClampsValuesOutsideRange() {
        assertThat(RiskScore.of(-0.5).value()).isZero();
        assertThat(RiskScore.of(1.5).value()).isEqualTo(1.0);
    }

    @Test
    void constructorRejectsValuesOutsideRange() {
        assertThatThrownBy(() -> new RiskScore(-0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Score deve estar entre 0.0 e 1.0");

        assertThatThrownBy(() -> new RiskScore(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringIncludesValueAndLevel() {
        assertThat(RiskScore.of(0.42).toString()).startsWith("RiskScore{value=").contains("MEDIUM");
    }
}
