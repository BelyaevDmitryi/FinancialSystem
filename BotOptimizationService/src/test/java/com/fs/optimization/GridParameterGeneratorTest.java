package com.fs.optimization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GridParameterGeneratorTest {

    private GridParameterGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new GridParameterGenerator();
    }

    @Test
    void generate_absoluteStep_period10to20Step2_returnsSixValues() {
        List<Double> values = generator.generate(10, 20, 2, StepType.ABSOLUTE);

        assertThat(values).containsExactly(10.0, 12.0, 14.0, 16.0, 18.0, 20.0);
    }

    @Test
    void generate_percentStep_increasesByPercentage() {
        List<Double> values = generator.generate(100, 130, 10, StepType.PERCENT);

        assertThat(values).hasSize(3);
        assertThat(values.get(0)).isCloseTo(100.0, within(1e-9));
        assertThat(values.get(1)).isCloseTo(110.0, within(1e-9));
        assertThat(values.get(2)).isCloseTo(121.0, within(1e-9));
    }

    @Test
    void generate_minGreaterThanMax_throws() {
        assertThatThrownBy(() -> generator.generate(20, 10, 1, StepType.ABSOLUTE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
