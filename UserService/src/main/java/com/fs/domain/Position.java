package com.fs.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class Position {
    private String ticker;
    private Integer quantity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return ticker.equals(position.ticker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker);
    }
}
