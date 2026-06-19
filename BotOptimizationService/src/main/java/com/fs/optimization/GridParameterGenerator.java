package com.fs.optimization;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GridParameterGenerator {

    public List<Double> generate(double min, double max, double step, StepType stepType) {
        if (min > max) {
            throw new IllegalArgumentException("min должен быть <= max");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step должен быть > 0");
        }
        List<Double> values = new ArrayList<>();
        if (stepType == StepType.ABSOLUTE) {
            for (double value = min; value <= max + 1e-9; value += step) {
                values.add(value);
            }
            return values;
        }
        for (double value = min; value <= max + 1e-9; value = value * (1.0 + step / 100.0)) {
            values.add(value);
        }
        return values;
    }
}
